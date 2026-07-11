/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.joinRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelJoinRequestsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChannelJoinRequestsState())
    val uiState: StateFlow<ChannelJoinRequestsState> = _uiState.asStateFlow()
    
    private val _effect = MutableSharedFlow<ChannelJoinRequestsEffect>()
    val effect: SharedFlow<ChannelJoinRequestsEffect> = _effect.asSharedFlow()
    
    private var channelId: Long = 0
    
    fun init(id: Long) {
        channelId = id
        loadRequests()
    }
    
    private fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = channelRepository.getJoinRequests(channelId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        requests = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(ChannelJoinRequestsEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
            }
        }
    }
    
    fun acceptRequest(userId: Long) {
        viewModelScope.launch {
            val result = channelRepository.acceptJoinRequest(channelId, userId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(requests = state.requests.filter { it.id != userId })
                }
            } else {
                _effect.emit(ChannelJoinRequestsEffect.ShowSnackbar(UiText.DynamicString("Ошибка при принятии заявки")))
            }
        }
    }
    
    fun rejectRequest(userId: Long) {
        viewModelScope.launch {
            val result = channelRepository.rejectJoinRequest(channelId, userId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(requests = state.requests.filter { it.id != userId })
                }
            } else {
                _effect.emit(ChannelJoinRequestsEffect.ShowSnackbar(UiText.DynamicString("Ошибка при отклонении заявки")))
            }
        }
    }
}
