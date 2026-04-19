/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
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
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()
    
    val socketState = webSocketClient.connectionState
    
    val hasPasscode = appLockManager.hasPasscode
    
    val user = userRepository.getMe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = User(
                id = 0,
                firstName = "",
                lastName = null,
                bio = null,
                username = null,
                dateOfBirth = null,
                lastSeen = null
            )
        )
    
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
                    chatRepository.refreshChats()
                }
            }
        }
    }
    
    private fun List<Chat>.sortedByLastMessage(): List<Chat> {
        return this.sortedByDescending { it.lastMessage?.sendTime ?: 0L }
    }
}
