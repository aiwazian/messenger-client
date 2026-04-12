/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.DeleteChatPayload
import com.aiwazian.messenger.domain.DeleteMessagePayload
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.ReadMessagePayload
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketAction
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val appLockManager: AppLockManager,
    userRepository: UserRepository,
    webSocketClient: WebSocketClient
) : ViewModel() {
    
    private val _uiEffect = MutableSharedFlow<MainUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    val socketState = webSocketClient.connectionState
    
    val hasPasscode = appLockManager.hasPasscode
    
    val user = userRepository.getMe()
    
    suspend fun lockApp() {
        appLockManager.lock()
    }
    
    fun showPermissionRationale() {
        viewModelScope.launch {
            _uiEffect.emit(MainUiEffect.ShowPermissionRationale)
        }
    }
    
    fun hidePermissionRationale() {
        viewModelScope.launch {
            _uiEffect.emit(MainUiEffect.HidePermissionRationale)
        }
    }
    
    fun openNotificationSettings() {
        viewModelScope.launch {
            _uiEffect.emit(MainUiEffect.OpenNotificationSettings)
        }
    }
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()
    
    init {
        viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                _chats.update { chats.sortedByLastMessage() }
            }
        }
        
        viewModelScope.launch {
            webSocketClient.connectionState.collectLatest {
                if (it == ConnectionState.CONNECTED) {
                    SessionManager.loadSession()
                }
            }
        }
        
        webSocketClient.subscribeToEvent<Message>(WebSocketAction.NEW_MESSAGE) { message ->
            onReceivingMessage(message)
        }
        
        webSocketClient.subscribeToEvent<DeleteMessagePayload>(WebSocketAction.DELETE_MESSAGE) { message ->
            onMessageDeleted(message.messageId, message.chatId)
        }
        
        webSocketClient.subscribeToEvent<ReadMessagePayload>(WebSocketAction.READ_MESSAGE) { message ->
            onReadMessage(message.chatId, message.messageId)
        }
        
        webSocketClient.subscribeToEvent<Chat>(WebSocketAction.NEW_CHAT) { chatInfo ->
            showNewChat(chatInfo)
        }
        
        webSocketClient.subscribeToEvent<DeleteChatPayload>(WebSocketAction.DELETE_CHAT) { payload ->
            deleteChat(payload.chatId)
        }
        
        webSocketClient.subscribeToEvent<DeleteChatPayload>(WebSocketAction.CHAT_REMOVED) { payload ->
            deleteChat(payload.chatId)
        }
        
        webSocketClient.subscribeToEvent<DeleteChatPayload>(WebSocketAction.CHAT_UPDATED) { payload ->
            viewModelScope.launch {
                val chatInfo = chatRepository.get(payload.chatId)
                if (chatInfo != null) {
                    chatRepository.saveChat(chatInfo)
                }
            }
        }
    }
    
    fun onSendMessage(message: Message) {
        processMessage(
            message.chatId, message
        )
    }
    
    fun onReceivingMessage(message: Message) {
        processMessage(message.chatId, message)
    }
    
    fun showNewChat(
        chat: Chat, lastMessage: Message? = null
    ) {
        viewModelScope.launch {
            lastMessage?.let { chatRepository.saveMessage(it) }
            chatRepository.syncChat(chat.copy(lastMessage = lastMessage))
        }
    }
    
    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }
    
    private fun onReadMessage(
        chatId: Long, messageId: Int
    ) {
        viewModelScope.launch {
            val currentChat = _chats.value.find { it.id == chatId }
            if (currentChat?.lastMessage?.id == messageId) {
                val lastMessage = currentChat.lastMessage.copy(isRead = true)
                chatRepository.saveMessage(lastMessage)
                chatRepository.syncChat(currentChat.copy(lastMessage = lastMessage))
            }
        }
    }
    
    private fun onMessageDeleted(
        messageId: Int, chatId: Long
    ) {
        viewModelScope.launch {
            val currentChat = _chats.value.find { it.id == chatId }
            if (currentChat?.lastMessage?.id == messageId) {
                val lastMessage = chatRepository.getLastMessage(chatId)
                lastMessage?.let { chatRepository.saveMessage(it) }
                chatRepository.syncChat(currentChat.copy(lastMessage = lastMessage))
            }
        }
    }
    
    private fun processMessage(
        chatId: Long, message: Message
    ) {
        viewModelScope.launch {
            chatRepository.saveMessage(message)
            val chat = _chats.value.find { it.id == chatId }
            
            if (chat == null) {
                val chatInfo = chatRepository.get(chatId)
                if (chatInfo != null) {
                    chatRepository.syncChat(chatInfo.copy(lastMessage = message))
                }
            } else {
                chatRepository.syncChat(chat.copy(lastMessage = message))
            }
        }
    }
    
    private fun List<Chat>.sortedByLastMessage(): List<Chat> {
        return this.sortedByDescending { it.lastMessage?.sendTime ?: 0L }
    }
}
