/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Досылает то, что не успело уйти до смерти процесса.
 *
 * Повторы живут в скоупе приложения и переживают уход из чата и сворачивание,
 * но не выгрузку процесса системой. Записи о начатых отправках лежат на диске,
 * поэтому после запуска остаётся поставить их в очередь заново — сообщение
 * доедет само, без ручного повтора.
 *
 * Поднимаются только отправки, чьё сообщение всё ещё лежит в чате: «Отменить
 * отправку» его удаляет, и запись без сообщения означает «передумали». Иначе
 * отменённые вложения возвращались бы в чат при каждом запуске.
 */
@Singleton
class PendingSendResumer @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val pendingSendStore: PendingSendStore,
    private val attachmentOutbox: AttachmentOutbox,
    private val messageDao: MessageDao,
    private val messageSendQueue: MessageSendQueue
) {
    
    /** Вызывается один раз при старте процесса. */
    fun resume() {
        appScope.launch {
            val resumed = mutableListOf<PendingSend>()
            
            pendingSendStore.all().forEach { send ->
                // Отправка, начатая уже в этом запуске: дублировать её незачем.
                if (messageSendQueue.isPending(send.tempId)) {
                    resumed.add(send)
                    return@forEach
                }
                
                // Сообщения в чате нет: отправку отменили ещё до перезапуска.
                if (isCancelled(send.tempId)) {
                    Log.i(TAG, "Send #${send.tempId} is cancelled, dropping it")
                    drop(send)
                    return@forEach
                }
                
                when {
                    send.uris.isNotEmpty() -> {
                        messageSendQueue.enqueueFiles(
                            chatId = send.chatId,
                            uris = send.uris,
                            text = send.text,
                            tempId = send.tempId,
                            replyTo = send.replyTo
                        )
                        
                        resumed.add(send)
                    }
                    
                    !send.text.isNullOrBlank() -> {
                        messageSendQueue.enqueueText(
                            chatId = send.chatId,
                            text = send.text,
                            tempId = send.tempId,
                            replyTo = send.replyTo
                        )
                        
                        resumed.add(send)
                    }
                    
                    // Ни файлов, ни текста: отправлять нечего.
                    else -> drop(send)
                }
            }
            
            attachmentOutbox.cleanUp(resumed.flatMap { it.uris })
        }
    }
    
    /**
     * Сбой чтения базы считаем за «сообщение на месте»: потерять отправку из-за
     * него хуже, чем лишний раз попробовать её дослать.
     */
    private suspend fun isCancelled(tempId: Long): Boolean = try {
        messageDao.getMessageById(tempId) == null
    } catch (e: Exception) {
        Log.e(TAG, "Unable to check message #$tempId", e)
        false
    }
    
    /** Запись и копии больше не нужны: поднимать из них нечего. */
    private suspend fun drop(send: PendingSend) {
        pendingSendStore.forget(send.tempId)
        send.uris.forEach { uri -> attachmentOutbox.release(uri) }
    }
    
    private companion object {
        const val TAG = "PendingSendResumer"
    }
}
