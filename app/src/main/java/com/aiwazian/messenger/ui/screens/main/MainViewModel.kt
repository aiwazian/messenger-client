/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.push.PushRegistrar
import com.aiwazian.messenger.repository.ChatFolderRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.ThemeManager
import com.aiwazian.messenger.utils.UiText
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
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val appLockManager: AppLockManager,
    private val themeManager: ThemeManager,
    userRepository: UserRepository,
    webSocketClient: WebSocketClient,
    private val onlineUsersTracker: OnlineUsersTracker,
    private val pushRegistrar: PushRegistrar
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    val socketState = webSocketClient.connectionState
    
    init {
        webSocketClient.connect()
        
        viewModelScope.launch {
            /*
             * Колокольчик в списке чатов считается на лету из настроек категорий и
             * исключений: переключение категории на экране настроек должно сразу
             * гасить или возвращать звук у всех её чатов, кроме исключений, без
             * похода на сервер. Исключение по чату сильнее его категории.
             */
            combine(
                chatRepository.getAllChats(),
                chatFolderRepository.getFolders(),
                notificationSettingsRepository.observe(),
                notificationSettingsRepository.observeChatExceptions()
            ) { chats, folders, settings, exceptions ->
                val overrides = exceptions.associate { it.chatId to it.enabled }
                val resolvedChats = chats.map { chat ->
                    val enabled = overrides[chat.id]
                        ?: settings.isEnabledFor(ChatType.fromId(chat.id))
                    if (chat.isMuted == !enabled) chat else chat.copy(isMuted = !enabled)
                }
                val sortedChats = resolvedChats.sortedByLastMessage()
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
                    notificationSettingsRepository.refresh()
                    notificationSettingsRepository.refreshChatExceptions()
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
        
        pushRegistrar.ensureRegistered()
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
    
    fun showAccountSheet() {
        _uiState.update { it.copy(showAccountBottomSheet = true) }
    }
    
    fun hideAccountSheet() {
        _uiState.update { it.copy(showAccountBottomSheet = false) }
    }
    
    fun setActiveFolder(folderId: Int) {
        _uiState.update { it.copy(activeFolderId = folderId) }
    }
    
    fun requestFolderDeletion(folderId: Int) {
        val folder = _uiState.value.folders.find { it.id == folderId } ?: return
        _uiState.update { it.copy(folderPendingDeletion = folder) }
    }
    
    fun cancelFolderDeletion() {
        _uiState.update { it.copy(folderPendingDeletion = null) }
    }
    
    fun confirmFolderDeletion() {
        val folder = _uiState.value.folderPendingDeletion ?: return
        _uiState.update { it.copy(folderPendingDeletion = null) }
        
        viewModelScope.launch {
            chatFolderRepository.deleteFolder(folder.id)
        }
    }
    
    private fun buildFolderPages(
        chats: List<Chat>,
        folders: List<ChatFolder>
    ): List<ChatFolderPage> {
        val allChatsPage = ChatFolderPage(
            id = ALL_CHATS_FOLDER_ID,
            name = UiText.StringResource(R.string.all_chats),
            chats = chats,
            unreadChatCount = chats.countUnread()
        )
        
        if (folders.isEmpty()) {
            return listOf(allChatsPage)
        }
        
        val folderPages = folders.map { folder ->
            val folderChats = chats
                .filter { folder.contains(it.id) }
                .sortedWith(
                    compareByDescending<Chat> { folder.isPinned(it.id) }
                        .thenByDescending { it.lastMessage?.sendTime ?: 0L }
                )
            
            ChatFolderPage(
                id = folder.id,
                name = UiText.DynamicString(folder.name),
                chats = folderChats,
                unreadChatCount = folderChats.countUnread()
            )
        }
        
        return listOf(allChatsPage) + folderPages
    }
    
    /**
     * Бейдж у названия папки считает непрочитанные чаты, а не сообщения. Пересчёт
     * идёт на каждой эмиссии потока чатов, поэтому прочтение чата, ручная пометка
     * прочитанным/непрочитанным и новое сообщение сразу меняют число.
     */
    private fun List<Chat>.countUnread(): Int {
        return this.count { it.isUnread }
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
    
    fun hasUnreadSelectedChats(): Boolean {
        val selectedIds = _uiState.value.selectedChatIds
        val chats = _uiState.value.chats
        return selectedIds.any { id ->
            chats.find { it.id == id }?.isUnread == true
        }
    }
    
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
    
    /**
     * «Прочитать все» из меню таба папки.
     *
     * На сервер уходят только те чаты папки, которые сейчас не прочитаны:
     * одним запросом на пачку, а не по запросу на каждый чат.
     */
    fun markFolderChatsRead(folderId: Int) {
        val page = _uiState.value.folderPages.find { it.id == folderId } ?: return
        val unreadIds = page.chats.filter { it.isUnread }.map { it.id }
        if (unreadIds.isEmpty()) {
            return
        }
        
        viewModelScope.launch {
            chatRepository.markChatsRead(unreadIds)
        }
    }
}
