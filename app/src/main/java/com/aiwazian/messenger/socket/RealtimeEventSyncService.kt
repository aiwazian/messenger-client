/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ReadMessagePayload
import com.aiwazian.messenger.enums.ChatType
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
    private val onlineUsersTracker: OnlineUsersTracker,
    private val notificationHelper: NotificationHelper
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
                
                val myId = userRepository.getMe().firstOrNull()?.id ?: return@launch
                if (message.senderId == myId) return@launch
                
                val chatId = resolveChatId(message.chatId, message.senderId, myId)
                
                chatRepository.incrementUnread(chatId, message.id)
                
                if (ActiveChatTracker.activeChatId.value != chatId) {
                    val chat = chatRepository.getById(chatId).firstOrNull()
                    val title =
                        chat?.chatName?.asString(context)
                            ?: context.getString(R.string.new_message)
                    val body = message.text ?: context.getString(R.string.message)
                    notificationHelper.showMessageNotification(
                        chatId,
                        title,
                        body,
                        chat?.avatarUri
                    )
                }
            }
        }
        
        webSocketClient.subscribeToEvent(WebSocketEvent.ChatUnread) { payload ->
            serviceScope.launch {
                chatRepository.applyUnreadState(
                    chatId = payload.chatId,
                    unreadCount = payload.unreadCount,
                    firstUnreadMessageId = payload.firstUnreadMessageId
                )
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
                if (ChatType.fromId(payload.chatId) == ChatType.GROUP) {
                    _groupReadEvents.emit(payload)
                    return@launch
                }
                
                if (payload.senderId > 0 && payload.sendTime > 0) {
                    chatRepository.markReadBySender(
                        payload.chatId, payload.senderId, payload.sendTime
                    )
                } else {
                    chatRepository.markMessageAsRead(payload.chatId, payload.messageId)
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
    
    /**
     * В какой чат положить входящее событие.
     *
     * id пользователя равен id его личного чата, поэтому в личной переписке смотреть надо
     * на senderId: Олег (id 1) пишет «в чат 2», и без пересчёта Андрей (id 2) кладёт это
     * сообщение себе в «Избранное». В группе и канале chatId общий для всех участников,
     * там берётся именно он.
     */
    private fun resolveChatId(chatId: Long, senderId: Long, myId: Long): Long {
        return when (ChatType.fromId(chatId)) {
            ChatType.PRIVATE -> if (chatId == myId) senderId else chatId
            else -> chatId
        }
    }
}
