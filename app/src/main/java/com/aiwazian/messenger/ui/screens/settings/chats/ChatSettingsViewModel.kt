/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatSettingsUiState(
    val pauseMusicOnRecord: Boolean = true,
    val pauseMusicOnMedia: Boolean = true,
    val raiseToListen: Boolean = true
)

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.getPauseMusicOnRecord().collectLatest { enabled ->
                _uiState.update { it.copy(pauseMusicOnRecord = enabled) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getPauseMusicOnMedia().collectLatest { enabled ->
                _uiState.update { it.copy(pauseMusicOnMedia = enabled) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getRaiseToListen().collectLatest { enabled ->
                _uiState.update { it.copy(raiseToListen = enabled) }
            }
        }
    }

    fun togglePauseMusicOnRecord(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.savePauseMusicOnRecord(enabled)
        }
    }

    fun togglePauseMusicOnMedia(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.savePauseMusicOnMedia(enabled)
        }
    }

    fun toggleRaiseToListen(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveRaiseToListen(enabled)
        }
    }
}
