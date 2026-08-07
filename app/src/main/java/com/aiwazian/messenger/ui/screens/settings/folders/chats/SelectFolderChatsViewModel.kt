/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders.chats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.ui.screens.settings.folders.FolderChatsSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectFolderChatsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SelectFolderChatsUiState())
    val uiState = _uiState.asStateFlow()
    
    private var allChats: List<Chat> = emptyList()
    private var selectionRestored = false
    
    init {
        viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                allChats = chats
                _uiState.update { state -> state.copy(chats = filterChats(chats, state.query)) }
            }
        }
    }
    
    /** Экран открывается с уже выбранным составом папки, чтобы выбор не терялся. */
    fun restoreSelection(chatIds: List<Long>, categories: List<ChatFolderCategory>) {
        if (selectionRestored) {
            return
        }
        selectionRestored = true
        
        _uiState.update { state ->
            state.copy(
                selectedChatIds = chatIds.toSet(),
                selectedCategories = categories.toSet()
            )
        }
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(query = query, chats = filterChats(allChats, query))
        }
    }
    
    fun toggleChat(chatId: Long) {
        _uiState.update { state ->
            val selected = if (chatId in state.selectedChatIds) {
                state.selectedChatIds - chatId
            } else {
                state.selectedChatIds + chatId
            }
            state.copy(selectedChatIds = selected)
        }
    }
    
    fun toggleCategory(category: ChatFolderCategory) {
        _uiState.update { state ->
            val selected = if (category in state.selectedCategories) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = selected)
        }
    }
    
    fun buildSelection(): FolderChatsSelection {
        val state = _uiState.value
        return FolderChatsSelection(
            chatIds = state.selectedChatIds.toList(),
            categories = state.selectedCategories.toList()
        )
    }
    
    private fun filterChats(chats: List<Chat>, query: String): List<Chat> {
        if (query.isBlank()) {
            return chats
        }
        
        return chats.filter { chat ->
            chat.chatName.asString(context).contains(query.trim(), ignoreCase = true)
        }
    }
}
