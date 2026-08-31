/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.usecase.SendMessageWithFilesUseCase
import com.aiwazian.messenger.utils.media.VideoQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отправка без экрана — для тех, кто закрывается сразу после нажатия
 * «Отправить».
 *
 * Системное «Поделиться» и шторка в профиле исчезают быстрее, чем успевает
 * уйти запрос, и ждать результат там просто некому. Сами повторы живут в
 * скоупе приложения внутри use case — см. [SendMessageUseCase] и
 * [SendMessageWithFilesUseCase], — поэтому очередь нужна не для того, чтобы
 * отправка выжила, а чтобы у неё был владелец: локальный id для отмены и
 * защита от повторного нажатия.
 */
@Singleton
class MessageSendQueue @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val pendingSendStore: PendingSendStore,
    private val attachmentOutbox: AttachmentOutbox,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMessageWithFilesUseCase: SendMessageWithFilesUseCase
) {
    
    private val jobs = ConcurrentHashMap<Long, Job>()
    
    /** Идёт ли отправка прямо сейчас: чтобы повторное нажатие не удвоило её. */
    fun isPending(tempId: Long): Boolean = jobs[tempId]?.isActive == true
    
    /**
     * Текстовое сообщение.
     *
     * @return локальный id, по которому отправку можно отменить.
     */
    fun enqueueText(
        chatId: Long,
        text: String,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null
    ): Long {
        enqueue(tempId) {
            sendMessageUseCase(
                chatId = chatId,
                message = text,
                tempId = tempId,
                replyTo = replyTo
            )
        }
        
        return tempId
    }
    
    /**
     * Файлы, фото, видео и голосовые.
     *
     * @param videoQualities ступени сжатия видео, выбранные в предпросмотре, по
     * исходным ссылкам. Чего в карте нет, уходит со ступенью по умолчанию.
     */
    fun enqueueFiles(
        chatId: Long,
        uris: List<Uri>,
        text: String? = null,
        tempId: Long = nextTempId(),
        replyTo: MessageReplyPreview? = null,
        videoQualities: Map<Uri, VideoQuality> = emptyMap()
    ): Long {
        enqueue(tempId) {
            sendMessageWithFilesUseCase(
                chatId = chatId,
                uris = uris,
                text = text,
                tempId = tempId,
                replyTo = replyTo,
                videoQualities = videoQualities
            )
        }
        
        return tempId
    }
    
    /**
     * Останавливает отправку и убирает за ней: пока запись о начатой отправке
     * лежит на диске, [PendingSendResumer] поднимет её при следующем запуске.
     */
    fun cancel(tempId: Long) {
        jobs.remove(tempId)?.cancel()
        
        // Отмена ожидания больше ничего не останавливает: сама отправка живёт в
        // скоупе приложения, и прервать её может только сам use case.
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
