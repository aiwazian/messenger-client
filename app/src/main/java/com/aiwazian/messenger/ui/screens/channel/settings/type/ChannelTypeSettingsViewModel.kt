/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.SearchRepository
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
    
    private var _channelId: Long = -1L
    private var checkLinkJob: Job? = null
    
    private val _uiState = MutableStateFlow(ChannelTypeSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelTypeSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _channelId = channelId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                channelRepository.getByIdFlow(channelId).collect { channel ->
                    _uiState.update {
                        it.copy(
                            channelId = channel.id,
                            isLoading = false,
                            channelType = channel.channelType,
                            publicLink = channel.username,
                            canSave = true,
                            linkCheckStatus = LinkCheckStatus.Idle
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.emit(
                    ChannelTypeSettingsEffect.ShowSnackbar(
                        e.message ?: "Ошибка загрузки канала"
                    )
                )
            }
        }
    }
    
    fun changeChannelType(channelType: ChannelType) {
        _uiState.update {
            it.copy(
                channelType = channelType,
                canSave = canSaveChannelType(channelType, it.publicLink, it.linkCheckStatus)
            )
        }
    }
    
    /**
     * Изменение публичной ссылки (username)
     */
    fun changePublicLink(publicLink: String) {
        _uiState.update {
            it.copy(
                publicLink = publicLink,
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
    
    /**
     * Проверка доступности публичной ссылки через SearchRepository
     */
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
        
        if (publicLink == _uiState.value.publicLink) {
            _uiState.update {
                it.copy(linkCheckStatus = LinkCheckStatus.Available, canSave = true)
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(linkCheckStatus = LinkCheckStatus.Checking) }
            
            try {
                val isAvailable = searchRepository.checkUsernameAvailable(publicLink)
                
                if (isAvailable) {
                    _uiState.update {
                        it.copy(
                            linkCheckStatus = LinkCheckStatus.Available,
                            canSave = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            linkCheckStatus = LinkCheckStatus.Busy,
                            canSave = false
                        )
                    }
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
    
    /**
     * Сохранение изменений типа канала
     */
    fun save() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (!canSaveChannelType(
                    currentState.channelType,
                    currentState.publicLink,
                    currentState.linkCheckStatus
                )
            ) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            try {
                channelRepository.updateChannelType(_channelId, currentState.channelType)
                    .onSuccess {
                        _uiEffect.emit(ChannelTypeSettingsEffect.NavigateBack)
                    }
                    .onFailure {
                        vibrationManager.vibrate(VibrationPattern.Error)
                        _uiEffect.emit(ChannelTypeSettingsEffect.ShowSnackbar("Не удалось сохранить"))
                    }
            } catch (_: Exception) {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(ChannelTypeSettingsEffect.ShowSnackbar("Ошибка сохранения"))
            }
        }
    }
    
    /**
     * Проверка возможности сохранения типа канала
     */
    private fun canSaveChannelType(
        channelType: ChannelType,
        publicLink: String?,
        status: LinkCheckStatus
    ): Boolean {
        if (channelType == ChannelType.PUBLIC) {
            if (publicLink.isNullOrBlank()) return false
            if (status == LinkCheckStatus.Busy) return false
            if (status is LinkCheckStatus.Error) return false
        }
        return true
    }
}
