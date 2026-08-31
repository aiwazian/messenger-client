/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.AttachmentUploadException
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.SendCancelledException
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.AttachmentOutbox
import com.aiwazian.messenger.utils.PendingSendStore
import com.aiwazian.messenger.utils.RetryPolicy
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отправка сообщения с вложениями: файлы, фото, видео и голосовые.
 *
 * Сама отправка идёт в скоупе приложения, поэтому уход из чата, поворот экрана
 * и сворачивание приложения её не прерывают: вызывающая сторона лишь ждёт
 * результат, и её отмена обрывает ожидание, а не загрузку. Остановить отправку
 * по-настоящему можно через [cancel]. Смерть процесса переживает запись в
 * [PendingSendStore] — после перезапуска отправка поднимается с тех же копий.
 *
 * Попыток столько, сколько понадобится. Одна попытка — это свежая ссылка на
 * загрузку плюс сама загрузка: форму подписывает сервер на ограниченное время,
 * поэтому после долгого обрыва сети повторять нужно с начала, а не с той же
 * формой. Пока попытки продолжаются, сообщение обязано оставаться
 * «отправляется», иначе восклицательный знак мигал бы в чате на каждой
 * неудаче.
 *
 * Отмена приходит сюда не вызовом, а исчезновением сообщения: «Отменить
 * отправку» живёт в модели экрана, удаляет локальное сообщение и про этот цикл
 * ничего не знает. Поэтому перед каждой попыткой сообщение проверяется в базе,
 * и если его больше нет — цикл останавливается и убирает копии вместе с
 * записью о начатой отправке. Без этой проверки запросы уходили бы вечно, а
 * запись поднимала бы отменённую отправку после каждого запуска.
 *
 * Статус ERROR остаётся ровно для отказов, которые повтор не изменит, — они
 * приходят как [AttachmentUploadException]: файл удалили, перенесли либо
 * отобрали к нему доступ, файл пуст (повреждён), файл не проходит по размеру.
 */
@Singleton
class SendMessageWithFilesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val messageDao: MessageDao,
    private val attachmentOutbox: AttachmentOutbox,
    private val pendingSendStore: PendingSendStore,
    private val uploadManager: UploadManager
) {
    private val running = ConcurrentHashMap<Long, Deferred<Result<Message>>>()
    
    suspend operator fun invoke(
        chatId: Long,
        uris: List<Uri>,
        text: String?,
        tempId: Long = -System.currentTimeMillis(),
        replyTo: MessageReplyPreview? = null,
        videoQualities: Map<Uri, VideoQuality> = emptyMap()
    ): Result<Message> {
        val sending = appScope.async(start = CoroutineStart.LAZY) {
            send(chatId, uris, text, tempId, replyTo, videoQualities)
        }
        
        running.put(tempId, sending)?.cancel()
        sending.invokeOnCompletion { running.remove(tempId, sending) }
        sending.start()
        
        return sending.await()
    }
    
    /** Останавливает отправку, которая продолжается в скоупе приложения. */
    fun cancel(tempId: Long) {
        running.remove(tempId)?.cancel()
    }
    
    private suspend fun send(
        chatId: Long,
        uris: List<Uri>,
        text: String?,
        tempId: Long,
        replyTo: MessageReplyPreview?,
        videoQualities: Map<Uri, VideoQuality>
    ): Result<Message> {
        val myId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        /*
         * Описание вложения снимается с исходника, потому что копий ещё нет и
         * появятся они не сразу: сжатие видео идёт десятками секунд.
         * Сообщение обязано попасть в чат до этого, иначе после нажатия
         * «Отправить» экран стоял бы пустым почти минуту. Настоящий вес копии
         * подтягивается ниже, когда копия готова, а имя для сервера берётся с неё
         * же прямо перед загрузкой.
         */
        val attachments = uris.mapIndexed { index, uri ->
            val fileName = fileNameOf(uri)
            val mimeType = uri.getFileType(context)
            
            val attachmentType = when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType.startsWith("video/") -> AttachmentType.VIDEO
                mimeType.startsWith("audio/") -> AttachmentType.VOICE
                else -> AttachmentType.FILE
            }
            
            MessageAttachment(
                fileId = "temp_${tempId}_$index",
                messageId = tempId,
                name = fileName,
                size = uri.getFileSize(context) ?: 0,
                extension = fileName.substringAfterLast('.', ""),
                status = DownloadStatus.UPLOADING,
                progress = 0,
                // Предпросмотр в чате остаётся на исходнике: он качественнее того,
                // что уйдёт на сервер, и не исчезает вместе с копией после отправки.
                localUri = uri,
                type = attachmentType,
                sortOrder = index
            )
        }
        
        val tempMessage = Message(
            id = tempId,
            text = text,
            senderId = myId,
            chatId = chatId,
            sendTime = System.currentTimeMillis(),
            isRead = false,
            status = MessageStatus.SENDING,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = attachments,
            replyTo = replyTo
        )
        
        chatRepository.saveLocalMessage(tempMessage)
        
        // Доступ к выбранному файлу живёт не дольше задачи приложения, а повторы
        // — сколько понадобится, поэтому грузим со своих копий. Фотографиям и
        // видео копией служит результат сжатия — этим занимается сам обменник.
        val sourceUris = uris.mapIndexed { index, uri ->
            attachmentOutbox.keep(
                uri = uri,
                key = "temp_${tempId}_$index",
                videoQuality = videoQualities[uri]
                    ?: MediaCompressionConfig.VIDEO_DEFAULT_QUALITY
            )
        }
        
        // Запись о начатой отправке появляется после самого сообщения, а не до:
        // смерть процесса между ними оставила бы запись без сообщения, и досылка
        // после перезапуска вернула бы в чат то, чего пользователь там не видел.
        pendingSendStore.remember(
            tempId = tempId,
            chatId = chatId,
            uris = sourceUris,
            text = text,
            replyTo = replyTo
        )
        
        syncSizes(attachments, sourceUris)
        
        val uploadResults = mutableListOf<AttachmentInputDto>()
        
        attachments.forEachIndexed { index, attachment ->
            val sourceUri = sourceUris[index]
            
            /*
             * Имя и mime-тип берутся с копии, а не из записи в базе: сжатое видео
             * уходит как mp4, а фотография как jpg. Спроси их у исходника — в
             * запросе на загрузку стояло бы video.mkv вместо video.mp4.
             */
            val fileName = fileNameOf(sourceUri)
            val mimeType = sourceUri.getFileType(context)
            
            // Идентификатор записи о файле меняется на серверный после каждой
            // выданной формы, и переименовывать дальше нужно уже его.
            var localFileId = attachment.fileId
            
            val uploadedFileId = RetryPolicy.retryForever(
                operation = "upload#$tempId/${attachment.fileId}",
                isPermanent = { it is AttachmentUploadException || it is SendCancelledException }
            ) {
                if (isCancelled(tempId)) {
                    return@retryForever Result.failure<String>(SendCancelledException(tempId))
                }
                
                // Размер спрашиваем заново на каждой попытке: у повреждённого
                // файла он нулевой, и сервер отказывает ещё на выдаче формы.
                val fileSize = sizeOf(sourceUri)
                
                if (fileSize <= 0) {
                    return@retryForever Result.failure<String>(
                        AttachmentUploadException.Empty(sourceUri.toString())
                    )
                }
                
                val initResponse = chatRepository.initFileUpload(
                    chatId, FileInitRequestDto(
                        name = fileName,
                        size = fileSize,
                        mimeType = mimeType,
                        category = attachment.type
                    )
                )
                
                if (initResponse == null) {
                    keepSending(tempId)
                    return@retryForever Result.failure<String>(
                        IOException("Unable to init upload for $fileName")
                    )
                }
                
                fileRepository.updateFileId(localFileId, initResponse.fileId)
                localFileId = initResponse.fileId
                
                val uploadResult = uploadManager.upload(
                    fileUri = sourceUri,
                    upload = initResponse,
                    fileId = initResponse.fileId,
                    maxAttempts = UploadManager.UNLIMITED_ATTEMPTS
                )
                
                if (uploadResult.isFailure) {
                    keepSending(tempId)
                }
                
                uploadResult.map { initResponse.fileId }
            }.getOrElse { error ->
                // Сюда попадаем только на безнадёжном отказе: обрывы сети и
                // просроченные формы отправка переживает сама.
                giveUp(tempId, sourceUris)
                
                if (error is SendCancelledException) {
                    Log.i(TAG, "Upload of $fileName dropped: send #$tempId is cancelled")
                    return Result.failure(error)
                }
                
                Log.e(TAG, "Upload of $fileName rejected for good", error)
                fileRepository.updateFileStatus(localFileId, DownloadStatus.FAILED)
                chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
                return Result.failure(error)
            }
            
            uploadResults.add(
                AttachmentInputDto(
                    fileId = uploadedFileId,
                    type = attachment.type
                )
            )
        }
        
        val result = RetryPolicy.retryForever(
            operation = "confirmUpload#$tempId",
            isPermanent = { it is AttachmentUploadException || it is SendCancelledException }
        ) {
            if (isCancelled(tempId)) {
                return@retryForever Result.failure<Message>(SendCancelledException(tempId))
            }
            
            val attempt = chatRepository.confirmFileUpload(
                chatId,
                uploadResults,
                text,
                replyTo?.messageId
            )
            
            if (attempt.isFailure) {
                keepSending(tempId)
            }
            
            attempt
        }
        
        result.onSuccess {
            // Первым делом: смерть процесса именно здесь отправила бы сообщение
            // второй раз после перезапуска.
            pendingSendStore.forget(tempId)
            sourceUris.forEach { uri -> attachmentOutbox.release(uri) }
            
            chatRepository.updateMessageId(tempId, it.id)
            val localChat = chatRepository.getById(chatId).firstOrNull()
            
            if (localChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }.onFailure { error ->
            giveUp(tempId, sourceUris)
            
            if (error is SendCancelledException) {
                Log.i(TAG, "Confirmation of #$tempId dropped: send is cancelled")
            } else {
                Log.e(TAG, "Confirmation failed", error)
                chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
            }
        }
        
        return result
    }
    
    /**
     * Имя файла с расширением.
     *
     * Расширение достаётся из mime-типа, если в имени его нет: без него чужой
     * клиент не поймёт, что скачал, а голосовые и вложения из некоторых
     * провайдеров приходят без расширения вовсе.
     */
    private fun fileNameOf(uri: Uri): String {
        val name = uri.getFileName(context) ?: DEFAULT_FILE_NAME
        
        if (name.contains('.')) {
            return name
        }
        
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(uri.getFileType(context))
        
        return if (extension != null) "$name.$extension" else name
    }
    
    /**
     * Подтягивает вес записи к весу копии.
     *
     * В чате у сжатого видео иначе висел бы вес исходника — всё время, пока
     * идёт загрузка, и потом число прыгало бы на втрое после ответа сервера.
     */
    private suspend fun syncSizes(
        attachments: List<MessageAttachment>,
        sourceUris: List<Uri>
    ) {
        attachments.forEachIndexed { index, attachment ->
            val size = sizeOf(sourceUris[index])
            
            if (size > 0 && size != attachment.size) {
                fileRepository.updateFileSize(attachment.fileId, size)
            }
        }
    }
    
    /**
     * Отправку отменили: кнопка удаляет локальное сообщение, и это единственный
     * след, который доходит до скоупа приложения.
     *
     * Сбой чтения базы считаем за «сообщение на месте»: оборвать из-за него
     * живую отправку хуже, чем сделать лишнюю попытку.
     */
    private suspend fun isCancelled(tempId: Long): Boolean = try {
        messageDao.getMessageById(tempId) == null
    } catch (e: Exception) {
        Log.e(TAG, "Unable to check message #$tempId", e)
        false
    }
    
    /**
     * Размер файла на диске. У своей копии он читается напрямую: спрашивать
     * размер file://-ссылки у ContentResolver незачем.
     */
    private fun sizeOf(uri: Uri): Long {
        if (uri.scheme == SCHEME_FILE) {
            return uri.path?.let { File(it).length() } ?: 0
        }
        
        return uri.getFileSize(context) ?: 0
    }
    
    /**
     * Неудачная попытка не должна проступать в чат: пока повторы продолжаются,
     * сообщение остаётся «отправляется», даже если репозиторий успел пометить
     * его ошибкой.
     */
    private suspend fun keepSending(tempId: Long) {
        chatRepository.updateMessageStatus(tempId, MessageStatus.SENDING)
    }
    
    /** Отправка окончена безнадёжно: ни копии, ни запись о ней больше не нужны. */
    private suspend fun giveUp(tempId: Long, sourceUris: List<Uri>) {
        pendingSendStore.forget(tempId)
        sourceUris.forEach { uri -> attachmentOutbox.release(uri) }
    }
    
    private companion object {
        const val TAG = "SendMessageWithFiles"
        const val SCHEME_FILE = "file"
        const val DEFAULT_FILE_NAME = "file"
    }
}
