/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectChannelViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SelectChannelUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<SelectChannelSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    init {
        loadChannels()
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = userRepository.getMe().firstOrNull()
            val selectedId = user?.profileChannelId
            
            userRepository.getOwnedChannels().onSuccess { channels ->
                _uiState.update {
                    it.copy(
                        channels = channels,
                        selectedChannelId = selectedId,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun selectChannel(channelId: Long) {
        viewModelScope.launch {
            userRepository.setProfileChannel(channelId).onSuccess {
                _uiState.update { it.copy(selectedChannelId = channelId) }
                _sideEffect.emit(SelectChannelSideEffect.NavigateBack)
            }.onFailure {
                _sideEffect.emit(
                    SelectChannelSideEffect.ShowSnackbar(
                        UiText.DynamicString("Не удалось выбрать канал")
                    )
                )
            }
        }
    }
    
    fun removeProfileChannel() {
        viewModelScope.launch {
            userRepository.removeProfileChannel().onSuccess {
                _uiState.update { it.copy(selectedChannelId = null) }
                _sideEffect.emit(SelectChannelSideEffect.NavigateBack)
            }.onFailure {
                _sideEffect.emit(
                    SelectChannelSideEffect.ShowSnackbar(
                        UiText.DynamicString("Не удалось скрыть канал")
                    )
                )
            }
        }
    }
}
