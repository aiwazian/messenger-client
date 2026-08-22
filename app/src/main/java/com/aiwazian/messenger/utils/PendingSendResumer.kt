/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

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
 */
@Singleton
class PendingSendResumer @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val pendingSendStore: PendingSendStore,
    private val attachmentOutbox: AttachmentOutbox,
    private val messageSendQueue: MessageSendQueue
) {
    
    /** Вызывается один раз при старте процесса. */
    fun resume() {
        appScope.launch {
            val pending = pendingSendStore.all()
            
            pending.forEach { send ->
                // Отправка, начатая уже в этом запуске: дублировать её незачем.
                if (messageSendQueue.isPending(send.tempId)) {
                    return@forEach
                }
                
                when {
                    send.uris.isNotEmpty() -> messageSendQueue.enqueueFiles(
                        chatId = send.chatId,
                        uris = send.uris,
                        text = send.text,
                        tempId = send.tempId,
                        replyTo = send.replyTo
                    )
                    
                    !send.text.isNullOrBlank() -> messageSendQueue.enqueueText(
                        chatId = send.chatId,
                        text = send.text,
                        tempId = send.tempId,
                        replyTo = send.replyTo
                    )
                    
                    // Ни файлов, ни текста: отправлять нечего.
                    else -> pendingSendStore.forget(send.tempId)
                }
            }
            
            attachmentOutbox.cleanUp(pending.flatMap { it.uris })
        }
    }
}
