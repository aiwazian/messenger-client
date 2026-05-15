/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelTypeSettingsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val searchRepository: SearchRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var checkLinkJob: Job? = null
    
    private val _uiState = MutableStateFlow(ChannelTypeSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelTypeSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        viewModelScope.launch {
            channelRepository.fetchById(channelId)
            channelRepository.getById(channelId).firstOrNull()?.let { channel ->
                _uiState.update {
                    it.copy(
                        channelId = channel.id,
                        channelType = channel.channelType,
                        username = channel.username.orEmpty(),
                        originalName = channel.username.orEmpty(),
                        canSave = true
                    )
                }
            }
        }
    }
    
    fun changeChannelType(channelType: ChannelType) {
        _uiState.update {
            it.copy(
                channelType = channelType,
                canSave = channelType == ChannelType.PRIVATE
            )
        }
    }
    
    fun onChangeUsername(newUsername: String) {
        val filteredUsername = newUsername.filter { it.toString().matches(RegexPatterns.USERNAME) }
        _uiState.update { it.copy(username = filteredUsername) }
        
        if (filteredUsername.isBlank()) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        if (filteredUsername.length < 5) {
            _uiState.update {
                it.copy(
                    isError = true,
                    canSave = false,
                    statusText = UiText.StringResource(R.string.min_length_5_characters),
                )
            }
            return
        }
        
        if (filteredUsername == _uiState.value.originalName) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        _uiState.update {
            it.copy(
                isError = false,
                canSave = false,
                statusText = UiText.DynamicString("Проверка")
            )
        }
        
        checkLinkJob?.cancel()
        checkLinkJob = viewModelScope.launch {
            delay(500)
            searchRepository.checkUsernameAvailable(filteredUsername).onSuccess { available ->
                if (available) {
                    _uiState.update {
                        it.copy(
                            canSave = true,
                            isError = false,
                            statusText = UiText.StringResource(R.string.username_available)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            canSave = false,
                            isError = true,
                            statusText = UiText.StringResource(R.string.username_taken)
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isError = true,
                        canSave = false,
                        statusText = UiText.StringResource(R.string.unexpected_error)
                    )
                }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            if (_uiState.value.username == _uiState.value.originalName) {
                _uiEffect.emit(ChannelTypeSettingsEffect.NavigateBack)
                return@launch
            }
            
            val currentState = _uiState.value
            channelRepository.updateChannelType(
                currentState.channelId,
                currentState.channelType,
                currentState.username
            ).onSuccess {
                _uiEffect.emit(ChannelTypeSettingsEffect.NavigateBack)
            }.onFailure {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(
                    ChannelTypeSettingsEffect.ShowSnackbar(
                        UiText.StringResource(R.string.failed_to_save_changes)
                    )
                )
            }
        }
    }
}
