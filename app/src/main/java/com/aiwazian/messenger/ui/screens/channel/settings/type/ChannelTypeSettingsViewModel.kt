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
            channelRepository.getById(channelId).collect { channel ->
                _uiState.update {
                    it.copy(
                        channelId = channel.id,
                        channelType = channel.channelType,
                        username = channel.username.orEmpty(),
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
                canSave = canSaveChannelType(channelType, it.username, it.linkCheckStatus)
            )
        }
    }
    
    fun changePublicLink(publicLink: String) {
        _uiState.update {
            it.copy(
                username = publicLink,
                linkCheckStatus = LinkCheckStatus.Idle,
                canSave = canSaveChannelType(it.channelType, publicLink, LinkCheckStatus.Idle)
            )
        }
        
        checkLinkJob?.cancel()
        checkLinkJob = viewModelScope.launch {
            delay(500)
            checkPublicLinkAvailability(publicLink)
        }
    }
    
    fun checkPublicLinkAvailability(publicLink: String) {
        if (publicLink.isBlank()) {
            _uiState.update {
                it.copy(
                    linkCheckStatus = LinkCheckStatus.Idle,
                    canSave = canSaveChannelType(it.channelType, publicLink, LinkCheckStatus.Idle)
                )
            }
            return
        }
        
        if (publicLink.length < 3) {
            _uiState.update {
                it.copy(
                    linkCheckStatus = LinkCheckStatus.Error("Минимальная длина 3 символа"),
                    canSave = false
                )
            }
            return
        }
        
        if (publicLink == _uiState.value.username) {
            _uiState.update {
                it.copy(linkCheckStatus = LinkCheckStatus.Available, canSave = true)
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(linkCheckStatus = LinkCheckStatus.Checking) }
            
            try {
                val isAvailable = searchRepository.checkUsernameAvailable(publicLink)
                
                _uiState.update {
                    it.copy(
                        linkCheckStatus = if (isAvailable) LinkCheckStatus.Available else LinkCheckStatus.Busy,
                        canSave = isAvailable
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        linkCheckStatus = LinkCheckStatus.Error("Не удалось проверить"),
                        canSave = false
                    )
                }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (!canSaveChannelType(
                    currentState.channelType,
                    currentState.username,
                    currentState.linkCheckStatus
                )
            ) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
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
    
    private fun canSaveChannelType(
        channelType: ChannelType,
        publicLink: String,
        status: LinkCheckStatus
    ): Boolean {
        if (channelType == ChannelType.PUBLIC) {
            return publicLink.isNotBlank() && (status == LinkCheckStatus.Available || status == LinkCheckStatus.Idle)
        }
        return true
    }
}
