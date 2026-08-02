/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.RuStorePushClient
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val appLockManager: AppLockManager,
    private val themeManager: ThemeManager,
    userRepository: UserRepository,
    webSocketClient: WebSocketClient,
    private val onlineUsersTracker: OnlineUsersTracker,
    private val accountDao: AccountDao,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    val socketState = webSocketClient.connectionState
    
    init {
        webSocketClient.connect()
        
        viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                _uiState.update { it.copy(chats = chats.sortedByLastMessage()) }
            }
        }
        
        viewModelScope.launch {
            webSocketClient.connectionState.collectLatest {
                if (it == ConnectionState.CONNECTED) {
                    SessionManager.loadSession()
                    userRepository.fetchMe()
                    chatRepository.refreshChats()
                    chatRepository.refreshOnlineUsers()
                }
            }
        }
        
        viewModelScope.launch {
            appLockManager.isLockApp.collectLatest { isLocked ->
                _uiState.update { it.copy(isLocked = isLocked) }
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
        
        viewModelScope.launch {
            onlineUsersTracker.onlineUsers.collectLatest { onlineIds ->
                _uiState.update { it.copy(onlineUserIds = onlineIds) }
            }
        }
        
        viewModelScope.launch {
            accountDao.getCurrentAccount()?.let { account ->
                RuStorePushClient.getToken().addOnSuccessListener { token ->
                    if (account.fcmToken != token) {
                        viewModelScope.launch {
                            sessionRepository.updateFcmToken(token).onSuccess {
                                accountDao.update(account.copy(fcmToken = token))
                                Log.d("MainViewModel", "Token updated")
                            }.onFailure {
                                Log.e("MainViewModel", "Error saving token", it)
                            }
                        }
                    }
                }.addOnFailureListener { th ->
                    Log.e("MainViewModel", "Error getting token", th)
                }
            }
        }
    }
    
    suspend fun lockApp() {
        appLockManager.lock()
    }
    
    fun showNotificationSheet() {
        _uiState.update { it.copy(showNotificationBottomSheet = true, askedPermission = true) }
    }
    
    fun hideNotificationSheet() {
        _uiState.update { it.copy(showNotificationBottomSheet = false) }
    }
    
    fun showAccountDialog() {
        _uiState.update { it.copy(showAccountDialog = true) }
    }
    
    fun hideAccountDialog() {
        _uiState.update { it.copy(showAccountDialog = false) }
    }
    
    private fun List<Chat>.sortedByLastMessage(): List<Chat> {
        return this.sortedWith(
            compareByDescending<Chat> { it.isPinned }
                .thenByDescending { it.lastMessage?.sendTime ?: 0L }
        )
    }
    
    fun toggleChatSelection(chatId: Long) {
        _uiState.update { state ->
            val newSelection = if (chatId in state.selectedChatIds) {
                state.selectedChatIds - chatId
            } else {
                state.selectedChatIds + chatId
            }
            state.copy(selectedChatIds = newSelection)
        }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedChatIds = emptySet()) }
    }
    
    fun pinSelectedChats() {
        val selectedIds = _uiState.value.selectedChatIds.toList()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.pinChats(selectedIds)
                clearSelection()
            }
        }
    }
    
    fun unpinSelectedChats() {
        val selectedIds = _uiState.value.selectedChatIds.toList()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.unpinChats(selectedIds)
                clearSelection()
            }
        }
    }
    
    fun hasUnpinnedSelectedChats(): Boolean {
        val selectedIds = _uiState.value.selectedChatIds
        val chats = _uiState.value.chats
        return selectedIds.any { id ->
            chats.find { it.id == id }?.isPinned == false
        }
    }
    
    /** Хотя бы один из выделенных чатов не прочитан. */
    fun hasUnreadSelectedChats(): Boolean {
        val selectedIds = _uiState.value.selectedChatIds
        val chats = _uiState.value.chats
        return selectedIds.any { id ->
            chats.find { it.id == id }?.isUnread == true
        }
    }
    
    /**
     * На сервер уходят только реально непрочитанные чаты: уже прочитанные из выделения
     * отсеиваются на клиенте.
     */
    fun markSelectedChatsRead() {
        val state = _uiState.value
        val unreadIds = state.selectedChatIds.filter { id ->
            state.chats.find { it.id == id }?.isUnread == true
        }
        if (unreadIds.isEmpty()) {
            clearSelection()
            return
        }
        viewModelScope.launch {
            chatRepository.markChatsRead(unreadIds)
            clearSelection()
        }
    }
    
    fun markSelectedChatsUnread() {
        val selectedIds = _uiState.value.selectedChatIds.toList()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.markChatsUnread(selectedIds)
                clearSelection()
            }
        }
    }
}
