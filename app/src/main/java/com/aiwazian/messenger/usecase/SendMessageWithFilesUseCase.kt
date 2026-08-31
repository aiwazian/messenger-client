/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
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
        replyTo: MessageReplyPreview? = null
    ): Result<Message> {
        val sending = appScope.async(start = CoroutineStart.LAZY) {
            send(chatId, uris, text, tempId, replyTo)
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
        replyTo: MessageReplyPreview?
    ): Result<Message> {
        val myId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        // Доступ к выбранному файлу живёт не дольше задачи приложения, а повторы
        // — сколько понадобится, поэтому грузим со своих копий. Фотографии копией
        // служит результат сжатия — этим занимается сам обменник.
        val sourceUris = uris.mapIndexed { index, uri ->
            attachmentOutbox.keep(uri, "temp_${tempId}_$index")
        }
        
        /*
         * Описание вложения снимается с копии, а не с исходника: сжатая
         * фотография уходит с другим именем, другим размером и другим
         * расширением. Спроси их у исходника — в запросе на загрузку стояло бы
         * photo.heic вместо photo.jpg и размер совсем другого файла, а сервер
         * сверяет размер ещё на выдаче формы.
         */
        val attachments = sourceUris.mapIndexed { index, sourceUri ->
            var fileName = sourceUri.getFileName(context) ?: "file"
            val fileSize = sourceUri.getFileSize(context) ?: 0
            val mimeType = sourceUri.getFileType(context)
            
            if (!fileName.contains('.')) {
                val extFromMime =
                    android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extFromMime != null) {
                    fileName = "$fileName.$extFromMime"
                }
            }
            
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
                size = fileSize,
                extension = fileName.substringAfterLast('.', ""),
                status = DownloadStatus.UPLOADING,
                progress = 0,
                // Предпросмотр в чате остаётся на исходнике: он качественнее того,
                // что уйдёт на сервер, и не исчезает вместе с копией после отправки.
                localUri = uris[index],
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
        
        val uploadResults = mutableListOf<AttachmentInputDto>()
        
        attachments.forEachIndexed { index, attachment ->
            val fileName = attachment.name
            val sourceUri = sourceUris[index]
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
    }
}
