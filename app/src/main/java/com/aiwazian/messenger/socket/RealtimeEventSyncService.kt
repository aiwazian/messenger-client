/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import com.aiwazian.messenger.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeEventSyncService @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val chatRepository: ChatRepository
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun start() {
        webSocketClient.subscribeToEvent(WebSocketEvent.NewMessage) { message ->
            serviceScope.launch {
                chatRepository.saveMessage(message)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.DeleteMessage) { payload ->
            serviceScope.launch {
                chatRepository.deleteMessage(payload.chatId, payload.messageId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ReadMessage) { payload ->
            serviceScope.launch {
                chatRepository.markMessageAsRead(payload.chatId, payload.messageId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.DeleteChat) { payload ->
            serviceScope.launch {
                chatRepository.deleteChat(payload.chatId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ChatRemoved) { payload ->
            serviceScope.launch {
                chatRepository.deleteChat(payload.chatId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.HistoryClear) { payload ->
            serviceScope.launch {
                chatRepository.clearLocalHistory(payload.chatId)
            }
        }
    }
}
