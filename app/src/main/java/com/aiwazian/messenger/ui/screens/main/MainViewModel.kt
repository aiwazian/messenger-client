/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
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
    private val themeManager: ThemeManager,
    userRepository: UserRepository,
    webSocketClient: WebSocketClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<MainUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    val socketState = webSocketClient.connectionState
    
    init {
        viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                _uiState.update { it.copy(chats = chats.sortedByLastMessage()) }
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
        
        viewModelScope.launch {
            appLockManager.hasPasscode.collectLatest { passcode ->
                _uiState.update { it.copy(hasPasscode = passcode) }
            }
        }
        
        viewModelScope.launch {
            themeManager.currentTheme.collectLatest { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        
        viewModelScope.launch {
            userRepository.getMe().collectLatest { me ->
                _uiState.update { it.copy(me = me) }
            }
        }
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
    
    private fun List<Chat>.sortedByLastMessage(): List<Chat> {
        return this.sortedByDescending { it.lastMessage?.sendTime ?: 0L }
    }
}
