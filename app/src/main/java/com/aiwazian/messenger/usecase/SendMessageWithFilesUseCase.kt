/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.domain.AttachmentUploadException
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReplyPreview
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
import com.aiwazian.messenger.utils.RetryPolicy
import com.aiwazian.messenger.utils.UploadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException
import javax.inject.Inject

/**
 * Отправка сообщения с вложениями: файлы, фото, видео и голосовые.
 *
 * Попыток столько, сколько понадобится. Одна попытка — это свежая ссылка на
 * загрузку плюс сама загрузка: форму подписывает сервер на ограниченное время,
 * поэтому после долгого обрыва сети повторять нужно с начала, а не с той же
 * формой. Пока попытки продолжаются, сообщение обязано оставаться
 * «отправляется», иначе восклицательный знак мигал бы в чате на каждой
 * неудаче.
 *
 * Статус ERROR остаётся ровно для отказов, которые повтор не изменит, — они
 * приходят как [AttachmentUploadException]: файл удалили, перенесли либо
 * отобрали к нему доступ, и файл не проходит по размеру.
 */
class SendMessageWithFilesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val uploadManager: UploadManager
) {
    suspend operator fun invoke(
        chatId: Long,
        uris: List<Uri>,
        text: String?,
        tempId: Long = -System.currentTimeMillis(),
        replyTo: MessageReplyPreview? = null
    ): Result<Message> {
        val myId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        val attachments = uris.mapIndexed { index, uri ->
            var fileName = uri.getFileName(context) ?: "file"
            val fileSize = uri.getFileSize(context) ?: 0
            val mimeType = uri.getFileType(context)
            
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
        
        val uploadResults = mutableListOf<AttachmentInputDto>()
        
        attachments.forEach { attachment ->
            val fileName = attachment.name
            val fileSize = attachment.size
            val sourceUri = attachment.localUri!!
            val mimeType = sourceUri.getFileType(context)
            
            // Идентификатор записи о файле меняется на серверный после каждой
            // выданной формы, и переименовывать дальше нужно уже его.
            var localFileId = attachment.fileId
            
            val uploadedFileId = RetryPolicy.retryForever(
                operation = "upload#$tempId/${attachment.fileId}",
                isPermanent = { it is AttachmentUploadException }
            ) {
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
        
        val result = RetryPolicy.retryForever("confirmUpload#$tempId") {
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
            chatRepository.updateMessageId(tempId, it.id)
            val localChat = chatRepository.getById(chatId).firstOrNull()
            
            if (localChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }.onFailure {
            Log.e(TAG, "Confirmation failed", it)
            chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
        }
        
        return result
    }
    
    /**
     * Неудачная попытка не должна проступать в чат: пока повторы продолжаются,
     * сообщение остаётся «отправляется», даже если репозиторий успел пометить
     * его ошибкой.
     */
    private suspend fun keepSending(tempId: Long) {
        chatRepository.updateMessageStatus(tempId, MessageStatus.SENDING)
    }
    
    private companion object {
        const val TAG = "SendMessageWithFiles"
    }
}
