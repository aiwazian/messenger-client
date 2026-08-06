/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
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
class CreateChatFolderViewModel @Inject constructor(
    private val chatFolderRepository: ChatFolderRepository,
    chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateChatFolderUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<CreateChatFolderSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private var allChats: List<Chat> = emptyList()
    
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
    
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
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
    
    fun createFolder() {
        val state = _uiState.value
        if (!state.canSave) {
            return
        }
        
        _uiState.update { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            chatFolderRepository.createFolder(
                name = state.name.trim(),
                chatIds = state.selectedChatIds,
                categories = state.selectedCategories
            ).onSuccess {
                _sideEffect.emit(CreateChatFolderSideEffect.FolderCreated)
            }.onFailure {
                _uiState.update { current -> current.copy(isSaving = false) }
            }
        }
    }
}
