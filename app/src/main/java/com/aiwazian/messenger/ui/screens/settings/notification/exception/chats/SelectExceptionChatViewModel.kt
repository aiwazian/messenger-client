/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception.chats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectExceptionChatViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SelectExceptionChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private var category: ChatFolderCategory? = null
    
    private var allChats: List<Chat> = emptyList()
    
    private var observeJob: Job? = null
    
    /**
     * Список ограничен категорией, из которой открыт экран: исключение по чату
     * другого типа в этот список потом не попадёт, и выглядело бы это так, будто
     * добавление не сработало.
     */
    fun init(category: ChatFolderCategory) {
        if (this.category == category) {
            return
        }
        
        this.category = category
        
        observeChats(category)
    }
    
    private fun observeChats(category: ChatFolderCategory) {
        observeJob?.cancel()
        
        observeJob = viewModelScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                allChats = chats.filter { category.matches(it.id) }
                
                _uiState.update { state ->
                    state.copy(chats = filterChats(allChats, state.query))
                }
            }
        }
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(query = query, chats = filterChats(allChats, query))
        }
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
