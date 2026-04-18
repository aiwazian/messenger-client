/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.subscribers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.ChannelRepository
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
class ChannelSubscribersViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var _channelId = -1L
    
    private val _uiState = MutableStateFlow(ChannelSubscribersUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelSubscribersSideEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _channelId = channelId
        viewModelScope.launch {
            if (channelId != -1L) {
                channelRepository.getSubscribers(_channelId).onSuccess {
                    _uiState.update { it.copy(subscribers = it.subscribers) }
                }
            }
        }
    }
    
    fun changeSearchQuery(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
    }
    
    fun kickUser() {
        viewModelScope.launch {
            val userId = _uiState.value.selectedUserId
            if (userId == null) {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Пользователь не выбран"))
                return@launch
            }
            
            channelRepository.kickUser(_channelId, userId).onSuccess {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Пользователь удален"))
            }.onFailure {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Не удалось удалить пользователя"))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun blockUser() {
        viewModelScope.launch {
            val userId = _uiState.value.selectedUserId
            if (userId == null) {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Пользователь не выбран"))
                return@launch
            }
            
            channelRepository.banUser(_channelId, userId).onSuccess {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Пользователь заблокирован"))
            }.onFailure {
                _uiEffect.emit(ChannelSubscribersSideEffect.ShowSnackbar("Не удалось заблокировать пользователя"))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun showKickDialog(userId: Long) {
        _uiState.update { it.copy(showKickDialog = true, selectedUserId = userId) }
    }
    
    fun showBlockDialog(userId: Long) {
        _uiState.update { it.copy(showBlockDialog = true, selectedUserId = userId) }
    }
    
    fun hideBlockDialog() {
        _uiState.update { it.copy(showBlockDialog = false) }
    }
    
    fun hideKickDialog() {
        _uiState.update { it.copy(showKickDialog = false) }
    }
}
