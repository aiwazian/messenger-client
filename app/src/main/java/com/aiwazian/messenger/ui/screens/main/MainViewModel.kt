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
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.UserManager
import com.aiwazian.messenger.utils.VibrationManager
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
    private val vibrationManager: VibrationManager,
    userManager: UserManager,
    webSocketClient: WebSocketClient
) : ViewModel() {
    
    private val _uiEffect = MutableSharedFlow<MainUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    val socketState = webSocketClient.connectionState
    
    val hasPasscode = appLockManager.hasPasscode
    
    val user = userManager.user
    
    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
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
            webSocketClient.connectionState.collectLatest {
                if (it == ConnectionState.CONNECTED) {
                    SessionManager.loadSession()
                    userManager.loadUserData()
                }
            }
        }
        
        webSocketClient.subscribeToTypedMessages<Message>(WebSocketAction.NEW_MESSAGE) { message ->
            onReceivingMessage(message)
        }
        
        webSocketClient.subscribeToTypedMessages<DeleteMessagePayload>(WebSocketAction.DELETE_MESSAGE) { message ->
            onMessageDeleted(
                message.messageId,
                message.chatId
            )
        }
        
        webSocketClient.subscribeToTypedMessages<ReadMessagePayload>(WebSocketAction.READ_MESSAGE) { message ->
            onReadMessage(
                message.chatId,
                message.messageId
            )
        }
        
        webSocketClient.subscribeToTypedMessages<Chat>(WebSocketAction.NEW_CHAT) { chatInfo ->
            showNewChat(chatInfo)
        }
        
        webSocketClient.subscribeToTypedMessages<DeleteChatPayload>(WebSocketAction.DELETE_CHAT) { payload ->
            deleteChat(payload.chatId)
        }
        
        viewModelScope.launch {
            webSocketClient.connectionState.collectLatest {
                if (it == ConnectionState.CONNECTED) {
                    loadChats()
                }
            }
        }
    }
    
    fun onSendMessage(message: Message) {
        processMessage(
            message.chatId,
            message
        )
    }
    
    fun onReceivingMessage(message: Message) {
        processMessage(
            message.senderId,
            message
        )
    }
    
    fun showNewChat(
        chat: Chat,
        lastMessage: Message? = null
    ) {
        val newChatInfo = chat.copy(lastMessage = lastMessage)
        
        _chats.update { currentChats ->
            (currentChats + newChatInfo).distinctBy { it.id }
        }
    }
    
    fun deleteChat(chatId: Long) {
        _chats.update { currentChats ->
            currentChats.filter { it.id != chatId }
        }
        
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }
    
    private fun onReadMessage(
        chatId: Long,
        messageId: Int
    ) {
        _chats.update { currentChats ->
            currentChats.map { chat ->
                if (chat.id == chatId && chat.lastMessage?.id == messageId) {
                    val lastMessage = chat.lastMessage.copy(isRead = true)
                    chat.copy(lastMessage = lastMessage)
                } else {
                    chat
                }
            }
        }
    }
    
    private fun onMessageDeleted(
        messageId: Int,
        chatId: Long
    ) {
        viewModelScope.launch {
            _chats.update { currentChats ->
                currentChats.map { chat ->
                    if (chat.id == chatId && chat.lastMessage?.id == messageId) {
                        val lastMessage = chatRepository.getLastMessage(chatId)
                        chat.copy(lastMessage = lastMessage)
                    } else {
                        chat
                    }
                }
            }
        }
    }
    
    private fun processMessage(
        chatId: Long,
        message: Message
    ) {
        val chatExists = _chats.value.any { it.id == chatId }
        
        if (!chatExists) {
            viewModelScope.launch {
                val chatInfo = chatRepository.get(chatId)
                
                if (chatInfo != null) {
                    showNewChat(
                        chatInfo,
                        message
                    )
                }
            }
        } else {
            updateLastMessage(
                chatId,
                message
            )
        }
    }
    
    private fun updateLastMessage(
        chatId: Long,
        lastMessage: Message
    ) {
        _chats.update { currentChats ->
            currentChats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(lastMessage = lastMessage)
                } else {
                    chat
                }
            }
        }
    }
    
    private suspend fun loadChats() {
        chatRepository.getAllChats().collectLatest { chats ->
            _chats.update { chats }
        }
    }
}



