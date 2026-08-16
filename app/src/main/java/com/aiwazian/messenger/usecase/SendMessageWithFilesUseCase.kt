/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
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
 * Каждый сетевой шаг повторяется по отдельности и без ограничения попыток:
 * пересоздавать локальное сообщение или заново загружать уже принятый файл при
 * обрыве сети не нужно. Статус ERROR остаётся только для отказов, которые повтор
 * не изменит: их отдаёт [UploadManager] — например, файл больше разрешённого
 * размера или хранилище ответило 4xx.
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
            val fileId = attachment.fileId
            val fileName = attachment.name
            val fileSize = attachment.size
            val mimeType = attachment.localUri!!.getFileType(context)
            
            // Ссылку на загрузку выдаёт сервер, и без неё загружать просто некуда,
            // поэтому запрашиваем столько раз, сколько понадобится.
            val initResponse = RetryPolicy.retryForever("initUpload#$tempId/$fileId") {
                val response = chatRepository.initFileUpload(
                    chatId, FileInitRequestDto(
                        name = fileName,
                        size = fileSize,
                        mimeType = mimeType,
                        category = attachment.type
                    )
                )
                
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(IOException("Unable to init upload for $fileName"))
                }
            }.getOrElse {
                chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
                return Result.failure(it)
            }
            
            fileRepository.updateFileId(fileId, initResponse.fileId)
            
            val uploadResult = uploadManager.upload(
                fileUri = attachment.localUri,
                upload = initResponse,
                fileId = initResponse.fileId,
                maxAttempts = UploadManager.UNLIMITED_ATTEMPTS
            )
            
            uploadResult.onSuccess {
                uploadResults.add(
                    AttachmentInputDto(
                        fileId = initResponse.fileId,
                        type = attachment.type
                    )
                )
            }.onFailure { error ->
                // Сюда попадаем только на безнадёжном отказе: обрывы сети
                // UploadManager переживает сам.
                Log.e("SendMessageWithFiles", "Upload rejected: ${error.message}", error)
                chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
                return Result.failure(error)
            }
        }
        
        val result = RetryPolicy.retryForever("confirmUpload#$tempId") {
            chatRepository.confirmFileUpload(
                chatId,
                uploadResults,
                text,
                replyTo?.messageId
            )
        }
        
        result.onSuccess {
            chatRepository.updateMessageId(tempId, it.id)
            val localChat = chatRepository.getById(chatId).firstOrNull()
            
            if (localChat == null) {
                chatRepository.fetchChatByIdFromServer(chatId)
            }
        }.onFailure {
            Log.e("SendMessageWithFiles", "Confirmation failed", it)
            chatRepository.updateMessageStatus(tempId, MessageStatus.ERROR)
        }
        
        return result
    }
}
