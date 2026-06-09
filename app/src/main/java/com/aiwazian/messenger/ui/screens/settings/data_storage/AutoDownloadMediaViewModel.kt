/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

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

data class AutoDownloadMediaUiState(
    val isAutoDownloadEnabled: Boolean = false,
    val isPhotoEnabled: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isFileEnabled: Boolean = true
)

@HiltViewModel
class AutoDownloadMediaViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AutoDownloadMediaUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            dataStoreManager.getAutoDownloadMedia().collectLatest { enabled ->
                _uiState.update { it.copy(isAutoDownloadEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getAutoDownloadPhotos().collectLatest { enabled ->
                _uiState.update { it.copy(isPhotoEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getAutoDownloadVideos().collectLatest { enabled ->
                _uiState.update { it.copy(isVideoEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getAutoDownloadFiles().collectLatest { enabled ->
                _uiState.update { it.copy(isFileEnabled = enabled) }
            }
        }
    }
    
    fun toggleAutoDownloadMedia(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoDownloadMedia(enabled)
        }
    }
    
    fun toggleAutoDownloadPhotos(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoDownloadPhotos(enabled)
        }
    }
    
    fun toggleAutoDownloadVideos(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoDownloadVideos(enabled)
        }
    }
    
    fun toggleAutoDownloadFiles(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoDownloadFiles(enabled)
        }
    }
}