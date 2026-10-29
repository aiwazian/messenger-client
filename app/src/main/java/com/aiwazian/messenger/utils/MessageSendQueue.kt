/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.ViewOnceOption
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageSendQueue @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val pendingSendStore: PendingSendStore,
    private val attachmentOutbox: AttachmentOutbox,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase
) {
    
    private val jobs = ConcurrentHashMap<Long, Job>()
    
    fun isPending(tempId: Long): Boolean = jobs[tempId]?.isActive == true

    fun enqueueText(
        chatId: Long,
        text: String,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null,
        silent: Boolean = false
    ): Long {
        enqueue(tempId) {
            sendMessageUseCase(
                chatId = chatId,
                message = text,
                tempId = tempId,
                replyTo = replyTo,
                silent = silent
            )
        }

        return tempId
    }

    fun enqueueFiles(
        chatId: Long,
        uris: List<Uri>,
        text: String? = null,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null,
        videoQualities: Map<Uri, VideoQuality> = emptyMap(),
        mediaTransforms: Map<Uri, MediaTransform> = emptyMap(),
        mediaDrawings: Map<Uri, Bitmap> = emptyMap(),
        viewOnceOptions: Map<Uri, ViewOnceOption> = emptyMap(),
        spoilerUris: Set<Uri> = emptySet(),
        silent: Boolean = false
    ): Long {
        enqueue(tempId) {
            sendMessageWithFilesUseCase(
                chatId = chatId,
                uris = uris,
                text = text,
                tempId = tempId,
                replyTo = replyTo,
                videoQualities = videoQualities,
                mediaTransforms = mediaTransforms,
                mediaDrawings = mediaDrawings,
                viewOnceOptions = viewOnceOptions,
                spoilerUris = spoilerUris,
                silent = silent
            )
        }

        return tempId
    }

    fun cancel(tempId: Long) {
        jobs.remove(tempId)?.cancel()
        
        sendMessageUseCase.cancel(tempId)
        sendMessageWithFilesUseCase.cancel(tempId)
        
        appScope.launch { forget(tempId) }
    }
    
    private suspend fun forget(tempId: Long) {
        val pending = pendingSendStore.find(tempId)
        
        pendingSendStore.forget(tempId)
        pending?.uris?.forEach { uri -> attachmentOutbox.release(uri) }
    }
    
    private fun enqueue(tempId: Long, block: suspend () -> Unit) {
        jobs.remove(tempId)?.cancel()
        
        val job = appScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sending #$tempId failed", e)
            }
        }
        
        jobs[tempId] = job
        job.invokeOnCompletion { jobs.remove(tempId, job) }
    }
    
    private fun nextTempId(): Long = -System.currentTimeMillis()
    
    private companion object {
        const val TAG = "MessageSendQueue"
    }
}
