/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.usecase.DeleteChannelUseCase
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelManagementViewModel @Inject constructor(
    private val deleteChannelUseCase: DeleteChannelUseCase,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChannelManagementUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelManagementEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _uiState.update { it.copy(channelId = channelId) }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun delete() {
        viewModelScope.launch {
            val channelId = _uiState.value.channelId
            
            if (channelId <= 0) {
                return@launch
            }
            
            if (deleteChannelUseCase(channelId)) {
                _uiEffect.emit(ChannelManagementEffect.NavigateToMain)
            } else {
                _uiState.update { it.copy(showDeleteDialog = false) }
                _uiEffect.emit(ChannelManagementEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_delete_channel)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
