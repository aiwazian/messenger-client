/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.UploadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SendMessageWithFilesUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val uploadManager: UploadManager
) {
    suspend operator fun invoke(chatId: Long, uris: List<Uri>, text: String?): Result<Message> {
        val tempId = -System.currentTimeMillis()
        val myId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        val attachments = uris.mapIndexed { index, uri ->
            val fileName = uri.getFileName(context) ?: "file"
            val fileSize = uri.getFileSize(context) ?: 0
            val mimeType = uri.getFileType(context)
            
            val attachmentType = when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType.startsWith("video/") -> AttachmentType.VIDEO
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
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = attachments
        )
        
        chatRepository.saveLocalMessage(tempMessage)
        
        val uploadResults = mutableListOf<AttachmentInputDto>()
        var success = true
        
        attachments.forEach { attachment ->
            val fileId = attachment.fileId
            val fileName = attachment.name
            val fileSize = attachment.size
            val mimeType = attachment.localUri!!.getFileType(context)
            
            val initResponse = chatRepository.initFileUpload(
                chatId, FileInitRequestDto(
                    name = fileName, size = fileSize, mimeType = mimeType
                )
            )
            
            if (initResponse == null) {
                success = false
                return@forEach
            }
            
            fileRepository.updateFileId(fileId, initResponse.fileId)
            
            uploadManager.upload(
                fileUri = attachment.localUri,
                uploadUrl = initResponse.signedUrl,
                fileId = initResponse.fileId,
            ).onSuccess {
                uploadResults.add(
                    AttachmentInputDto(
                        fileId = initResponse.fileId,
                        type = attachment.type
                    )
                )
            }.onFailure {
                Log.e("SendMessageWithFiles", "Upload failed", it)
                success = false
            }
        }
        
        return if (success) {
            val result = chatRepository.confirmFileUpload(chatId, uploadResults, text)
            result.onSuccess {
                chatRepository.updateMessageId(tempId, it.id)
                val localChat = chatRepository.getById(chatId).firstOrNull()
                
                if (localChat == null) {
                    chatRepository.refreshChats()
                }
            }.onFailure {
                Log.e("SendMessageWithFiles", "Confirmation failed", it)
            }
            result
        } else {
            Result.failure(Exception("Upload failed"))
        }
    }
}
