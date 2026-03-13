package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.DeleteChatPayload
import com.aiwazian.messenger.data.DeleteMessagePayload
import com.aiwazian.messenger.data.FolderInfo
import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.data.ReadMessagePayload
import com.aiwazian.messenger.database.repository.ChatRepository
import com.aiwazian.messenger.database.repository.FolderRepository
import com.aiwazian.messenger.enums.WebSocketAction
import com.aiwazian.messenger.services.AppLockService
import com.aiwazian.messenger.utils.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val folderRepository: FolderRepository,
    private val appLockService: AppLockService
) : ViewModel() {
    
    val hasPasscode = appLockService.hasPasscode
    
    suspend fun lockApp() {
        appLockService.lock()
    }
    
    private val _archivedChats = MutableStateFlow<List<ChatInfo>>(emptyList())
    val archivedChats = _archivedChats.asStateFlow()
    
    /** Folder id - Chat id **/
    private val _selectedChats = MutableStateFlow<Map<Int, List<Long>>>(emptyMap())
    val selectedChats = _selectedChats.asStateFlow()
    
    private val _chatFolders = MutableStateFlow<Set<FolderInfo>>(emptySet())
    val chatFolders = _chatFolders.asStateFlow()
    
    private val _allSelectedArePinned = MutableStateFlow(false)
    val allSelectedArePinned = _allSelectedArePinned.asStateFlow()
    
    init {
        WebSocketManager.registerMessageHandler<Message>(WebSocketAction.NEW_MESSAGE) { message ->
            onReceivingMessage(message)
        }
        
        WebSocketManager.registerMessageHandler<DeleteMessagePayload>(WebSocketAction.DELETE_MESSAGE) { message ->
            onMessageDeleted(
                message.messageId,
                message.chatId
            )
        }
        
        WebSocketManager.registerMessageHandler<ReadMessagePayload>(WebSocketAction.READ_MESSAGE) { message ->
            onReadMessage(
                message.chatId,
                message.messageId
            )
        }
        
        WebSocketManager.registerMessageHandler<ChatInfo>(WebSocketAction.NEW_CHAT) { chatInfo ->
            showNewChat(chatInfo)
        }
        
        WebSocketManager.registerMessageHandler<DeleteChatPayload>(WebSocketAction.DELETE_CHAT) { payload ->
            deleteChat(payload.chatId)
        }
        
        viewModelScope.launch {
            tryLoadArchiveChats()
        }
        
        viewModelScope.launch {
            folderRepository.loadFolders()
        }
        
        viewModelScope.launch {
            folderRepository.folders.collectLatest { folders ->
                _chatFolders.update { folders.toSet() }
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
    
    fun selectChat(
        chatId: Long,
        folderId: Int
    ) {
        _selectedChats.update { currentMap ->
            val existingFolders = currentMap[folderId] ?: emptyList()
            
            if (chatId !in existingFolders) {
                val updatedFolders = existingFolders + chatId
                currentMap + (folderId to updatedFolders)
            } else {
                val updatedFolders = existingFolders - chatId
                
                if (updatedFolders.isEmpty()) {
                    currentMap - folderId
                } else {
                    currentMap + (folderId to updatedFolders)
                }
            }
        }
        
        _allSelectedArePinned.update { true }
        
        _selectedChats.value.forEach { folder ->
            val selectedChatFolder = _chatFolders.value.find { it.id == folder.key }
            
            if (selectedChatFolder == null) {
                return@forEach
            }
            
            folder.value.forEach { selectedChatId ->
                val selectedChat = selectedChatFolder.chats.find { it.id == selectedChatId }
                
                if (selectedChat == null) {
                    return@forEach
                }
                
                if (!selectedChat.isPinned) {
                    _allSelectedArePinned.update { false }
                    return
                }
            }
        }
    }
    
    fun unselectAllChats() {
        _selectedChats.update { emptyMap() }
    }
    
    suspend fun pinChat(
        chatId: Long,
        folderId: Int
    ) {
        val folder = _chatFolders.value.find { it.id == folderId }
        val chatToPin = folder?.chats?.find { it.id == chatId }
        
        if (chatToPin == null || chatToPin.isPinned) {
            return
        }
        
        _chatFolders.update { currentFolderList ->
            currentFolderList.map { currentFolder ->
                if (currentFolder.id == folderId) {
                    val pinnedChat = chatToPin.copy(isPinned = true)
                    
                    val newChatList = currentFolder.chats.filter { it.id != chatId }
                        .toMutableList()
                    
                    newChatList.add(pinnedChat)
                    
                    val pinnedChats = newChatList.filter { it.isPinned }
                        .toMutableList()
                    val unpinnedChats = newChatList.filter { !it.isPinned }
                    
                    pinnedChats.sortByDescending { it.lastMessage?.sendTime }
                    
                    val finalChats = pinnedChats + unpinnedChats
                    
                    currentFolder.copy(chats = finalChats)
                } else {
                    currentFolder
                }
            }
                .toSet()
        }
        
        try {
            chatRepository.pin(
                chatId,
                folderId
            )
        } catch (e: Exception) {
            Log.e(
                "MainViewModel",
                "Ошибка при закреплении чата",
                e
            )
        }
    }
    
    suspend fun unpinChat(
        chatId: Long,
        folderId: Int
    ) {
        val folder = _chatFolders.value.find { it.id == folderId }
        val chatToUnpin = folder?.chats?.find { it.id == chatId }
        
        if (chatToUnpin == null || !chatToUnpin.isPinned) {
            return
        }
        
        _chatFolders.update { currentFolderList ->
            currentFolderList.map { currentFolder ->
                if (currentFolder.id == folderId) {
                    val updatedChats = currentFolder.chats.toMutableList()
                    
                    updatedChats.remove(chatToUnpin)
                    
                    val unpinnedChat = chatToUnpin.copy(isPinned = false)
                    
                    updatedChats.add(unpinnedChat)
                    
                    val pinnedChats = updatedChats.filter { it.isPinned }
                    val unpinnedChats = updatedChats.filter { !it.isPinned }
                    
                    val sortedUnpinnedChats =
                        unpinnedChats.sortedByDescending { it.lastMessage?.sendTime }
                    
                    val finalChats = pinnedChats + sortedUnpinnedChats
                    
                    currentFolder.copy(chats = finalChats)
                } else {
                    currentFolder
                }
            }
                .toSet()
        }
        
        try {
            chatRepository.unpin(
                chatId,
                folderId
            )
        } catch (e: Exception) {
            Log.e(
                "MainViewModel",
                "Ошибка при откреплении чата" + e.message
            )
        }
    }
    
    fun archiveChat(chatId: Long) {
        _chatFolders.update { currentFolders ->
            currentFolders.map { folder ->
                if (folder.id == 0) {
                    val chat = folder.chats.find { it.id == chatId }
                    
                    if (chat != null) {
                        _archivedChats.update { currentArchived ->
                            (currentArchived + chat).distinctBy { it.id }
                        }
                    }
                    
                    val chats = folder.chats.filter { it.id != chatId }
                    folder.copy(chats = chats)
                } else {
                    folder
                }
            }
                .toSet()
        }
        
        viewModelScope.launch {
            try {
                chatRepository.archive(chatId)
            } catch (e: Exception) {
                Log.e(
                    "MainViewModel",
                    "Ошибка при архивировании чата",
                    e
                )
            }
        }
    }
    
    fun unarchiveChat(chatId: Long) {
        var chatToUnarchive: ChatInfo? = null
        
        _archivedChats.update { currentArchived ->
            val mutableList = currentArchived.toMutableList()
            val index = mutableList.indexOfFirst { it.id == chatId }
            
            if (index == -1) {
                currentArchived
            } else {
                chatToUnarchive = mutableList.removeAt(index)
                mutableList
            }
        }
        
        if (chatToUnarchive == null) {
            return
        }
        
        showNewChat(
            chatToUnarchive,
            chatToUnarchive.lastMessage
        )
        
        viewModelScope.launch {
            try {
                chatRepository.unarchive(chatId)
            } catch (e: Exception) {
                Log.e(
                    "MainViewModel",
                    "Ошибка при разархивировании чата",
                    e
                )
            }
        }
    }
    
    fun showNewChat(
        chatInfo: ChatInfo,
        lastMessage: Message? = null
    ) {
        val newChatInfo = chatInfo.copy(lastMessage = lastMessage)
        
        _chatFolders.update { currentFolders ->
            currentFolders.map { folder ->
                if (folder.id == 0) {
                    val pinnedChats = folder.chats.filter { it.isPinned }
                    val unpinnedChats = folder.chats.filter { !it.isPinned }
                    
                    val updatedUnpinnedChats = unpinnedChats.toMutableList()
                    updatedUnpinnedChats.add(newChatInfo)
                    
                    updatedUnpinnedChats.sortByDescending { it.lastMessage?.sendTime }
                    
                    val updatedChats = pinnedChats + updatedUnpinnedChats
                    folder.copy(chats = updatedChats)
                } else {
                    folder
                }
            }
                .toSet()
        }
    }
    
    fun deleteChat(chatId: Long) {
        _chatFolders.update { currentFolders ->
            currentFolders.map { folder ->
                folder.copy(chats = folder.chats.filter { it.id != chatId })
            }
                .toSet()
        }
        
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
        }
    }
    
    private fun onReadMessage(
        chatId: Long,
        messageId: Int
    ) {
        _chatFolders.update { currentFolders ->
            currentFolders.map { folder ->
                val chatToUpdate = folder.chats.find { it.id == chatId }
                
                if (chatToUpdate == null || chatToUpdate.lastMessage?.id != messageId) {
                    return@map folder
                }
                
                try {
                    if (chatToUpdate.lastMessage == null) {
                        return
                    }
                    
                    val lastMessage = chatToUpdate.lastMessage!!.copy(isRead = true)
                    
                    val updatedChat = chatToUpdate.copy(lastMessage = lastMessage)
                    
                    val updatedChats = folder.chats.map {
                        if (it.id == chatId) updatedChat else it
                    }
                    
                    folder.copy(chats = updatedChats)
                } catch (e: Exception) {
                    Log.e(
                        "MainScreenViewModel",
                        "Error updating last message after deletion",
                        e
                    )
                    folder
                }
            }
                .toSet()
        }
    }
    
    private fun onMessageDeleted(
        messageId: Int,
        chatId: Long
    ) {
        viewModelScope.launch {
            _chatFolders.update { currentFolders ->
                currentFolders.map { folder ->
                    val chatToUpdate = folder.chats.find { it.id == chatId }
                    
                    if (chatToUpdate == null || chatToUpdate.lastMessage?.id != messageId) {
                        return@map folder
                    }
                    
                    try {
                        val lastMessage = chatRepository.getLastMessage(chatId)
                        
                        val updatedChat = chatToUpdate.copy(lastMessage = lastMessage)
                        
                        val updatedChats = folder.chats.map {
                            if (it.id == chatId) updatedChat else it
                        }
                        
                        folder.copy(chats = updatedChats)
                    } catch (e: Exception) {
                        Log.e(
                            "MainScreenViewModel",
                            "Error updating last message after deletion",
                            e
                        )
                        folder
                    }
                }
                    .toSet()
            }
        }
    }
    
    private fun processMessage(
        chatId: Long,
        message: Message
    ) {
        var chatFound = false
        
        _chatFolders.value.forEach { folder ->
            if (folder.chats.any { it.id == chatId }) {
                updateLastMessage(
                    chatId,
                    message
                )
                chatFound = true
                return@forEach
            }
        }
        
        if (!chatFound) {
            viewModelScope.launch {
                val chatInfo = chatRepository.get(chatId)
                
                if (chatInfo != null) {
                    showNewChat(
                        chatInfo,
                        message
                    )
                }
            }
        }
    }
    
    private fun updateLastMessage(
        chatId: Long,
        lastMessage: Message
    ) {
        _chatFolders.update { currentFolders ->
            currentFolders.map { folder ->
                val chatToUpdate = folder.chats.find { it.id == chatId }
                
                if (chatToUpdate == null) {
                    return@map folder
                }
                
                val updatedChat = chatToUpdate.copy(lastMessage = lastMessage)
                
                val otherChats = folder.chats.filter { it.id != chatId }
                
                val pinnedChats =
                    (otherChats.filter { it.isPinned } + (if (updatedChat.isPinned) updatedChat else null)).filterNotNull()
                        .sortedByDescending { it.lastMessage?.sendTime }
                
                val unpinnedChats =
                    (otherChats.filter { !it.isPinned } + (if (!updatedChat.isPinned) updatedChat else null)).filterNotNull()
                        .sortedByDescending { it.lastMessage?.sendTime }
                
                val finalChats = pinnedChats + unpinnedChats
                
                folder.copy(chats = finalChats)
            }
                .toSet()
        }
    }
    
    private suspend fun tryLoadArchiveChats() {
        try {
            val response = RetrofitInstance.api.getArchivedChats()
            
            val responseBody = response.body()
            
            if (!response.isSuccessful || responseBody == null) {
                return
            }
            
            _archivedChats.update { responseBody }
        } catch (e: Exception) {
            Log.e(
                "MainViewModel",
                "Ошибка при загрузке архивных чатов",
                e
            )
        }
    }
}
