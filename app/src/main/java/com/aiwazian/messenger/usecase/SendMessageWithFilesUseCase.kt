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
import com.aiwazian.messenger.extensions.MediaDimensions
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.getMediaDimensions
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
import com.aiwazian.messenger.utils.media.MediaTransform
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
        videoQualities: Map<Uri, VideoQuality> = emptyMap(),
        mediaTransforms: Map<Uri, MediaTransform> = emptyMap()
    ): Result<Message> {
        val sending = appScope.async(start = CoroutineStart.LAZY) {
            send(chatId, uris, text, tempId, replyTo, videoQualities, mediaTransforms)
        }
        
        running.put(tempId, sending)?.cancel()
        sending.invokeOnCompletion { running.remove(tempId, sending) }
        sending.start()
        
        return sending.await()
    }
    
    fun cancel(tempId: Long) {
        running.remove(tempId)?.cancel()
    }
    
    private suspend fun send(
        chatId: Long,
        uris: List<Uri>,
        text: String?,
        tempId: Long,
        replyTo: MessageReplyPreview?,
        videoQualities: Map<Uri, VideoQuality>,
        mediaTransforms: Map<Uri, MediaTransform>
    ): Result<Message> {
        val myId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        val attachments = uris.mapIndexed { index, uri ->
            val fileName = fileNameOf(uri)
            val mimeType = uri.getFileType(context)
            
            val attachmentType = when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType.startsWith("video/") -> AttachmentType.VIDEO
                mimeType.startsWith("audio/") -> AttachmentType.VOICE
                else -> AttachmentType.FILE
            }
            
            val frame = uri.getMediaDimensions(context, mimeType)
            
            MessageAttachment(
                fileId = "temp_${tempId}_$index",
                messageId = tempId,
                name = fileName,
                size = uri.getFileSize(context) ?: 0,
                extension = fileName.substringAfterLast('.', ""),
                status = DownloadStatus.UPLOADING,
                progress = 0,
                localUri = uri,
                type = attachmentType,
                sortOrder = index,
                width = frame?.width,
                height = frame?.height
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
        
        val sourceUris = uris.mapIndexed { index, uri ->
            attachmentOutbox.keep(
                uri = uri,
                key = "temp_${tempId}_$index",
                videoQuality = videoQualities[uri]
                    ?: MediaCompressionConfig.VIDEO_DEFAULT_QUALITY,
                transform = mediaTransforms[uri] ?: MediaTransform.None
            )
        }
        
        pendingSendStore.remember(
            tempId = tempId,
            chatId = chatId,
            uris = sourceUris,
            text = text,
            replyTo = replyTo
        )
        
        val sourceFrames = sourceUris.map { uri ->
            uri.getMediaDimensions(context, uri.getFileType(context))
        }
        
        val localFrames = sourceFrames.mapIndexed { index, frame ->
            if (mediaTransforms[uris[index]]?.swapsSides == true) null else frame
        }
        
        syncLocalFiles(attachments, sourceUris, localFrames)
        
        val uploadResults = mutableListOf<AttachmentInputDto>()
        
        attachments.forEachIndexed { index, attachment ->
            val sourceUri = sourceUris[index]
            val frame = sourceFrames[index]
            
            val fileName = fileNameOf(sourceUri)
            val mimeType = sourceUri.getFileType(context)
            
            var localFileId = attachment.fileId
            
            val uploadedFileId = RetryPolicy.retryForever(
                operation = "upload#$tempId/${attachment.fileId}",
                isPermanent = { it is AttachmentUploadException || it is SendCancelledException }
            ) {
                if (isCancelled(tempId)) {
                    return@retryForever Result.failure(SendCancelledException(tempId))
                }
                
                val fileSize = sizeOf(sourceUri)
                
                if (fileSize <= 0) {
                    return@retryForever Result.failure(
                        AttachmentUploadException.Empty(sourceUri.toString())
                    )
                }
                
                val initResponse = chatRepository.initFileUpload(
                    chatId, FileInitRequestDto(
                        name = fileName,
                        size = fileSize,
                        mimeType = mimeType,
                        category = attachment.type,
                        width = frame?.width,
                        height = frame?.height
                    )
                )
                
                if (initResponse == null) {
                    keepSending(tempId)
                    return@retryForever Result.failure(
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
                return@retryForever Result.failure(SendCancelledException(tempId))
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
            pendingSendStore.forget(tempId)
            sourceUris.forEach { uri -> attachmentOutbox.release(uri) }
            
            chatRepository.updateMessageId(tempId, it.id)
            
            chatRepository.updateMessageStatus(it.id, MessageStatus.SENT)
            
            uploadResults.forEach { uploaded ->
                fileRepository.updateFileStatus(uploaded.fileId, DownloadStatus.UPLOADED)
            }
            
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
    
    private fun fileNameOf(uri: Uri): String {
        val name = uri.getFileName(context) ?: DEFAULT_FILE_NAME
        
        if (name.contains('.')) {
            return name
        }
        
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(uri.getFileType(context))
        
        return if (extension != null) "$name.$extension" else name
    }
    
    private suspend fun syncLocalFiles(
        attachments: List<MessageAttachment>,
        sourceUris: List<Uri>,
        frames: List<MediaDimensions?>
    ) {
        attachments.forEachIndexed { index, attachment ->
            val size = sizeOf(sourceUris[index])
            
            if (size > 0 && size != attachment.size) {
                fileRepository.updateFileSize(attachment.fileId, size)
            }
            
            val frame = frames[index] ?: return@forEachIndexed
            
            if (frame.width != attachment.width || frame.height != attachment.height) {
                fileRepository.updateFileDimensions(
                    attachment.fileId,
                    frame.width,
                    frame.height
                )
            }
        }
    }
    
    private suspend fun isCancelled(tempId: Long): Boolean = try {
        messageDao.getMessageById(tempId) == null
    } catch (e: Exception) {
        Log.e(TAG, "Unable to check message #$tempId", e)
        false
    }
    
    private fun sizeOf(uri: Uri): Long {
        if (uri.scheme == SCHEME_FILE) {
            return uri.path?.let { File(it).length() } ?: 0
        }
        
        return uri.getFileSize(context) ?: 0
    }
    
    private suspend fun keepSending(tempId: Long) {
        chatRepository.updateMessageStatus(tempId, MessageStatus.SENDING)
    }
    
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
