/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.repository.ChatFolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatFoldersViewModel @Inject constructor(
    private val chatFolderRepository: ChatFolderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatFoldersUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            chatFolderRepository.getFolders().collectLatest { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
        
        viewModelScope.launch {
            chatFolderRepository.refreshFolders()
        }
    }
    
    fun requestFolderDeletion(folder: ChatFolder) {
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
}
