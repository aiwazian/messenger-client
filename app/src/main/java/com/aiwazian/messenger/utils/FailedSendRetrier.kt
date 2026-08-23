/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Досылка текстовых сообщений, чья отправка прервалась.
 *
 * Цикл повторов живёт в скоупе приложения и переживает уход с экрана, но не
 * смерть процесса: после неё сообщение остаётся в базе со статусом
 * «отправляется» или «ошибка», и досылать его уже некому. Держать ради этого
 * запись на диске незачем — текст целиком лежит в самом сообщении, поэтому
 * недоотправленные сообщения просто перечитываются из базы при открытии чата.
 *
 * Вложения так не поднять: путь к исходному файлу после перезапуска уже не
 * читается, поэтому файловые сообщения оставлены [PendingSendResumer] с его
 * записями и копиями в [AttachmentOutbox].
 */
@Singleton
class FailedSendRetrier @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val messageDao: MessageDao,
    private val messageSendQueue: MessageSendQueue,
    private val sendMessageUseCase: SendMessageUseCase
) {
    
    /** Следит за открытым чатом. Вызывается один раз при старте приложения. */
    fun start() {
        appScope.launch {
            ActiveChatTracker.activeChatId.filterNotNull().collect { chatId ->
                retryIn(chatId)
            }
        }
    }
    
    private suspend fun retryIn(chatId: Long) {
        val unsent = try {
            unsentMessages(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Reading unsent messages of chat $chatId failed", e)
            return
        }
        
        unsent.forEach { row ->
            val message = row.message
            
            if (message.status != MessageStatus.SENDING && message.status != MessageStatus.ERROR) {
                return@forEach
            }
            
            // Отправка ещё идёт: повторная постановка оборвала бы её на полпути.
            if (messageSendQueue.isPending(message.id) || sendMessageUseCase.isRunning(message.id)) {
                return@forEach
            }
            
            // Файлы восстанавливает PendingSendResumer по своим записям: здесь
            // исходных uri уже нет, и повтор упал бы с той же ошибкой.
            if (row.attachments.isNotEmpty()) {
                return@forEach
            }
            
            val text = message.text
            
            if (text.isNullOrBlank()) {
                return@forEach
            }
            
            Log.d(TAG, "Resending text message #${message.id}")
            
            messageSendQueue.enqueueText(
                chatId = message.chatId,
                text = text,
                tempId = message.id,
                replyTo = message.toDomain().replyTo
            )
        }
    }
    
    /**
     * Локальные сообщения чата, которые ещё не ушли на сервер.
     *
     * Отдельного запроса под это в базе нет, но окно сообщений умеет добавлять к
     * выборке локальные отправляемые — у них отрицательный id. Границы окна
     * заданы пустым диапазоном, от максимума до минимума, поэтому обычные
     * сообщения с сервера не попадают в выборку вовсе: остаются только
     * недоотправленные, от старых к новым.
     */
    private suspend fun unsentMessages(chatId: Long) = messageDao.getChatMessagesWindow(
        chatId = chatId,
        fromId = Long.MAX_VALUE,
        toId = Long.MIN_VALUE,
        includePending = 1
    ).first()
    
    private companion object {
        const val TAG = "FailedSendRetrier"
    }
}
