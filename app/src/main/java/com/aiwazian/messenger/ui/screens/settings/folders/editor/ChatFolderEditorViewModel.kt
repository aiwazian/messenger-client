/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.repository.ChatFolderRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.ui.screens.settings.folders.FolderChatsSelection
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
class ChatFolderEditorViewModel @Inject constructor(
    private val chatFolderRepository: ChatFolderRepository,
    chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatFolderEditorUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<ChatFolderEditorSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private var allChats: List<Chat> = emptyList()
    private var isFolderLoaded = false
    
    init {
        viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                allChats = chats
                _uiState.update { state ->
                    state.copy(chats = chats.filter { chat -> chat.id in state.selectedChatIds })
                }
            }
        }
    }
    
    /**
     * Содержимое папки читается ровно один раз: дальше состояние принадлежит
     * экрану, иначе обновление из Room затирало бы несохранённые правки.
     */
    fun loadFolder(folderId: Int?) {
        if (isFolderLoaded) {
            return
        }
        
        isFolderLoaded = true
        
        if (folderId == null) {
            return
        }
        
        viewModelScope.launch {
            val folder = loadFromCacheOrNetwork(folderId) ?: return@launch
            val chatIds = folder.includedChatIds
            
            _uiState.update { state ->
                state.copy(
                    folderId = folder.id,
                    name = folder.name,
                    selectedChatIds = chatIds,
                    selectedCategories = folder.categories,
                    chats = allChats.filter { chat -> chat.id in chatIds }
                )
            }
        }
    }
    
    /** Лишние символы отсекаются прямо на вводе: длиннее вкладка не помещается. */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name.take(MAX_FOLDER_NAME_LENGTH)) }
    }
    
    /** Результат экрана выбора чатов: список приходит целиком и заменяет прежний. */
    fun applySelection(selection: FolderChatsSelection) {
        _uiState.update { state ->
            state.copy(
                selectedChatIds = selection.chatIds,
                selectedCategories = selection.categories,
                chats = allChats.filter { chat -> chat.id in selection.chatIds }
            )
        }
    }
    
    fun saveFolder() {
        val state = _uiState.value
        if (!state.canSave) {
            return
        }
        
        _uiState.update { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            val folderId = state.folderId
            
            val result = if (folderId == null) {
                chatFolderRepository.createFolder(
                    name = state.name.trim(),
                    chatIds = state.selectedChatIds,
                    categories = state.selectedCategories
                )
            } else {
                chatFolderRepository.updateFolder(
                    folderId = folderId,
                    name = state.name.trim(),
                    chatIds = state.selectedChatIds,
                    categories = state.selectedCategories
                )
            }
            
            result.onSuccess {
                _sideEffect.emit(ChatFolderEditorSideEffect.FolderSaved)
            }.onFailure {
                _uiState.update { current -> current.copy(isSaving = false) }
            }
        }
    }
    
    /** Папки открывают из списка, но на холодном старте Room может быть ещё пуст. */
    private suspend fun loadFromCacheOrNetwork(folderId: Int): ChatFolder? {
        chatFolderRepository.getFolder(folderId)?.let { return it }
        chatFolderRepository.refreshFolders()
        return chatFolderRepository.getFolder(folderId)
    }
}
