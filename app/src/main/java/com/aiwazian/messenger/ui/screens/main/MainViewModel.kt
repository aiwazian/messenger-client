/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.repository.ChatFolderRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import com.aiwazian.messenger.utils.UiText
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatFolderRepository: ChatFolderRepository,
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
            combine(
                chatRepository.getAllChats(),
                chatFolderRepository.getFolders()
            ) { chats, folders ->
                val sortedChats = chats.sortedByLastMessage()
                Triple(sortedChats, folders, buildFolderPages(sortedChats, folders))
            }.collectLatest { (chats, folders, folderPages) ->
                _uiState.update {
                    it.copy(chats = chats, folders = folders, folderPages = folderPages)
                }
            }
        }
        
        viewModelScope.launch {
            webSocketClient.connectionState.collectLatest {
                if (it == ConnectionState.CONNECTED) {
                    SessionManager.loadSession()
                    userRepository.fetchMe()
                    chatRepository.refreshChats()
                    chatRepository.refreshOnlineUsers()
                    chatFolderRepository.refreshFolders()
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
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
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
    
    /** Открытая вкладка решает, где именно закреплять выделенные чаты. */
    fun setActiveFolder(folderId: Int) {
        _uiState.update { it.copy(activeFolderId = folderId) }
    }
    
    /**
     * Вкладка «Все чаты» идёт первой всегда. Остальные вкладки появляются, только если
     * у пользователя есть собственные папки: иначе пейджер на главном экране не нужен.
     */
    private fun buildFolderPages(
        chats: List<Chat>,
        folders: List<ChatFolder>
    ): List<ChatFolderPage> {
        val allChatsPage = ChatFolderPage(
            id = ALL_CHATS_FOLDER_ID,
            name = UiText.StringResource(R.string.all_chats),
            chats = chats
        )
        
        if (folders.isEmpty()) {
            return listOf(allChatsPage)
        }
        
        val folderPages = folders.map { folder ->
            ChatFolderPage(
                id = folder.id,
                name = UiText.DynamicString(folder.name),
                chats = chats
                    .filter { folder.contains(it.id) }
                    .sortedWith(
                        // Глобальный Chat.isPinned здесь намеренно не учитывается.
                        compareByDescending<Chat> { folder.isPinned(it.id) }
                            .thenByDescending { it.lastMessage?.sendTime ?: 0L }
                    )
            )
        }
        
        return listOf(allChatsPage) + folderPages
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
        setSelectedChatsPinned(true)
    }
    
    fun unpinSelectedChats() {
        setSelectedChatsPinned(false)
    }
    
    /**
     * Закрепление принадлежит вкладке: во «Всех чатах» меняется общий флаг чата,
     * внутри папки — только её собственный список закреплённых.
     */
    private fun setSelectedChatsPinned(isPinned: Boolean) {
        val state = _uiState.value
        val selectedIds = state.selectedChatIds.toList()
        if (selectedIds.isEmpty()) {
            return
        }
        
        val folderId = state.activeFolderId
        
        viewModelScope.launch {
            if (folderId == ALL_CHATS_FOLDER_ID) {
                if (isPinned) {
                    chatRepository.pinChats(selectedIds)
                } else {
                    chatRepository.unpinChats(selectedIds)
                }
            } else {
                if (isPinned) {
                    chatFolderRepository.pinChats(folderId, selectedIds)
                } else {
                    chatFolderRepository.unpinChats(folderId, selectedIds)
                }
            }
            
            clearSelection()
        }
    }
    
    fun hasUnpinnedSelectedChats(): Boolean {
        val state = _uiState.value
        val folder = state.folders.find { it.id == state.activeFolderId }
        
        return state.selectedChatIds.any { id ->
            if (folder == null) {
                state.chats.find { it.id == id }?.isPinned == false
            } else {
                !folder.isPinned(id)
            }
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
