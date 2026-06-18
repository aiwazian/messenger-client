/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ReadMessagePayload
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.ActiveChatTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeEventSyncService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    webSocketClient: WebSocketClient,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val onlineUsersTracker: OnlineUsersTracker
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _groupReadEvents = MutableSharedFlow<ReadMessagePayload>(extraBufferCapacity = 16)
    val groupReadEvents = _groupReadEvents.asSharedFlow()
    
    private val _chatRemovedEvents = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 16)
    val chatRemovedEvents = _chatRemovedEvents.asSharedFlow()
    
    init {
        webSocketClient.subscribeToEvent(WebSocketEvent.NewMessage) { message ->
            serviceScope.launch {
                chatRepository.saveLocalMessage(message)
                
                val myId = userRepository.getMe().firstOrNull()?.id
                val chatId = if (message.chatId == myId) message.senderId else message.chatId
                if (myId != null && message.senderId != myId) {
                    if (ActiveChatTracker.activeChatId.value == chatId) {
                        chatRepository.markAllAsRead(chatId)
                    } else {
                        val chat = chatRepository.getById(chatId).firstOrNull()
                        val title =
                            chat?.chatName?.asString(context)
                                ?: context.getString(R.string.new_message)
                        val body = message.text ?: context.getString(R.string.message)
                        NotificationHelper.showMessageNotification(
                            context,
                            chatId,
                            title,
                            body,
                            chat?.avatarUri
                        )
                    }
                }
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.DeleteMessage) { payload ->
            serviceScope.launch {
                chatRepository.deleteLocalMessage(payload.messageId)
            }
        }

        webSocketClient.subscribeToEvent(WebSocketEvent.MessageEdit) { message ->
            serviceScope.launch {
                chatRepository.updateLocalMessage(
                    messageId = message.id,
                    text = message.text,
                    editedAt = message.editedAt
                )
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ReadMessage) { payload ->
            serviceScope.launch {
                chatRepository.markMessageAsRead(payload.chatId, payload.messageId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ChatRead) { payload ->
            serviceScope.launch {
                val chatType = com.aiwazian.messenger.enums.ChatType.fromId(payload.chatId)
                if (chatType == com.aiwazian.messenger.enums.ChatType.GROUP) {
                    _groupReadEvents.emit(payload)
                } else {
                    if (payload.senderId > 0 && payload.sendTime > 0) {
                        chatRepository.markReadBySender(
                            payload.chatId, payload.senderId, payload.sendTime
                        )
                    } else {
                        chatRepository.markMessageAsRead(payload.chatId, payload.messageId)
                    }
                }
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ChatRemoved) { payload ->
            serviceScope.launch {
                chatRepository.deleteLocalChat(payload.chatId)
                _chatRemovedEvents.emit(payload.chatId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.NewChat) { chat ->
            serviceScope.launch {
                chatRepository.fetchChatByIdFromServer(chat.id)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.HistoryClear) { payload ->
            serviceScope.launch {
                chatRepository.clearLocalHistory(payload.chatId)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.PinChat) { payload ->
            serviceScope.launch {
                chatRepository.updateChatPinnedStatus(payload.chatIds, true)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.UnpinChat) { payload ->
            serviceScope.launch {
                chatRepository.updateChatPinnedStatus(payload.chatIds, false)
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.UserOnline) { payload ->
            onlineUsersTracker.setOnline(payload.userId)
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.UserOffline) { payload ->
            onlineUsersTracker.setOffline(payload.userId)
        }
    }
}
