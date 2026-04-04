/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateInviteLinkViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateInviteLinkUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CreateInviteLinkEffect>()
    val effect = _effect.asSharedFlow()

    private var channelId: Long = -1

    fun init(channelId: Long) {
        this.channelId = channelId
    }

    fun onMaxUsesChange(value: String) {
        _uiState.update { it.copy(maxUses = value) }
    }

    fun onExpirationDateChange(date: Long?) {
        _uiState.update { it.copy(expirationDate = date, showDatePicker = false) }
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun createLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val maxUses = _uiState.value.maxUses.toIntOrNull()
            val expirationDate = _uiState.value.expirationDate
            
            val expiresInSeconds = if (expirationDate != null) {
                ((expirationDate - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            } else null
            
            val result = channelRepository.createInviteLink(channelId, maxUses, expiresInSeconds)
            
            if (result.isSuccess) {
                _effect.emit(CreateInviteLinkEffect.Success)
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
