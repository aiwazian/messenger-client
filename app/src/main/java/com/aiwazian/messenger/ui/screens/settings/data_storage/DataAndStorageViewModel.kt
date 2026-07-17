/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataAndStorageViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataAndStorageUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<DataAndStorageUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun clearAllDrafts() {
        viewModelScope.launch {
            chatRepository.deleteAllDrafts()
            _uiEffect.emit(
                DataAndStorageUiEffect.ShowSnackbar(
                    UiText.StringResource(R.string.drafts_cleared)
                )
            )
        }
    }
    
    fun showClearDraftsDialog() {
        _uiState.update { it.copy(showClearDraftsDialog = true) }
    }
    
    fun hideClearDraftsDialog() {
        _uiState.update { it.copy(showClearDraftsDialog = false) }
    }
}
