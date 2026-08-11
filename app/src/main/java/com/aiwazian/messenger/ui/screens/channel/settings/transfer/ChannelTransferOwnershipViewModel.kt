/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.transfer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.ChannelRepository
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
class ChannelTransferOwnershipViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChannelTransferOwnershipUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelTransferOwnershipEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _uiState.update { it.copy(channelId = channelId) }
        
        viewModelScope.launch {
            channelRepository.getSubscribers(channelId).onSuccess { subscribers ->
                _uiState.update { it.copy(subscribers = subscribers, isLoading = false) }
            }.onFailure { error ->
                Log.e(TAG, "error load subscribers", error)
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(
                    ChannelTransferOwnershipEffect.ShowSnackbar(
                        UiText.StringResource(R.string.unexpected_error)
                    )
                )
            }
        }
    }
    
    fun selectUser(user: User) {
        _uiState.update { it.copy(selectedUser = user) }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedUser = null) }
    }
    
    fun confirmTransfer() {
        val state = _uiState.value
        val user = state.selectedUser ?: return
        
        if (state.isTransferring) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true) }
            
            channelRepository.transferOwnership(state.channelId, user.id).onSuccess {
                _uiState.update { it.copy(isTransferring = false, selectedUser = null) }
                _uiEffect.emit(ChannelTransferOwnershipEffect.NavigateToMain)
            }.onFailure { error ->
                Log.e(TAG, "error transfer ownership", error)
                _uiState.update { it.copy(isTransferring = false, selectedUser = null) }
                _uiEffect.emit(
                    ChannelTransferOwnershipEffect.ShowSnackbar(
                        UiText.StringResource(R.string.transfer_ownership_failed)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private companion object {
        const val TAG = "ChannelTransferOwnershipViewModel"
    }
}
