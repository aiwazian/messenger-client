/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class ChannelTypeSettingsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val chatRepository: ChatRepository,
    private val searchRepository: SearchRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private var _channelId: Long = -1L
    private var checkLinkJob: Job? = null

    private val _uiState = MutableStateFlow(ChannelTypeSettingsUiState())
    val uiState: StateFlow<ChannelTypeSettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChannelTypeSettingsEffect>()
    val uiEffect: SharedFlow<ChannelTypeSettingsEffect> = _uiEffect.asSharedFlow()

    private var isInitialized = false
    
    fun init(channelId: Long) {
        if (isInitialized) return
        isInitialized = true
        _channelId = channelId

        viewModelScope.launch {
            loadChannel(channelId)
        }
    }
    
    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Фоновое сетевое обновление
                viewModelScope.launch {
                    channelRepository.getById(channelId).collect {}
                }

                // Реактивное обновление из Room
                channelRepository.getByIdFlow(channelId).collect { channelInfo ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            channel = channelInfo,
                            channelType = ChannelType.fromInt(channelInfo.channelType),
                            publicLink = channelInfo.username ?: "",
                            inviteLink = channelInfo.inviteLink,
                            canSave = true,
                            linkCheckStatus = LinkCheckStatus.Idle
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки канала"
                    )
                }
                _uiEffect.emit(ChannelTypeSettingsEffect.ShowError(e.message ?: "Ошибка загрузки канала"))
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

        // Если ссылка не изменилась, она доступна
        if (publicLink == _uiState.value.channel.username) {
            _uiState.update {
                it.copy(
                    linkCheckStatus = LinkCheckStatus.Available,
                    canSave = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(linkCheckStatus = LinkCheckStatus.Checking)
            }

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
            } catch (e: Exception) {
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

            // Проверка валидности
            if (!canSaveChannelType(currentState.channelType, currentState.publicLink, currentState.linkCheckStatus)) {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(ChannelTypeSettingsEffect.VibrateError)
                return@launch
            }

            try {
                // Обновление данных канала
                val updatedChannel = currentState.channel.copy(
                    channelType = currentState.channelType.ordinal,
                    username = if (currentState.channelType == ChannelType.PUBLIC) {
                        currentState.publicLink.trim()
                    } else {
                        null
                    }
                )

                val result = channelRepository.update(updatedChannel)

                if (result.isSuccess) {
                    _uiEffect.emit(ChannelTypeSettingsEffect.VibrateSuccess)
                    _uiEffect.emit(ChannelTypeSettingsEffect.NavigateBack)
                } else {
                    vibrationManager.vibrate(VibrationPattern.Error)
                    _uiEffect.emit(ChannelTypeSettingsEffect.VibrateError)
                    _uiEffect.emit(ChannelTypeSettingsEffect.ShowError("Не удалось сохранить"))
                }
            } catch (e: Exception) {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(ChannelTypeSettingsEffect.VibrateError)
                _uiEffect.emit(ChannelTypeSettingsEffect.ShowError(e.message ?: "Ошибка сохранения"))
            }
        }
    }

    fun resetInviteLink() {
        viewModelScope.launch {
            val newLink = chatRepository.resetInviteLink(_channelId)
            if (newLink != null) {
                _uiState.update { it.copy(inviteLink = newLink) }
            } else {
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }

    /**
     * Проверка возможности сохранения типа канала
     */
    private fun canSaveChannelType(channelType: ChannelType, publicLink: String, status: LinkCheckStatus): Boolean {
        if (channelType == ChannelType.PUBLIC) {
            if (publicLink.isBlank()) return false
            if (status == LinkCheckStatus.Busy) return false
            if (status is LinkCheckStatus.Error) return false
        }
        return true
    }
}
