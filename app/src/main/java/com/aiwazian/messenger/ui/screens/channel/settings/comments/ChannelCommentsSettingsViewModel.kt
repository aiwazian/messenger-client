/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
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
class ChannelCommentsSettingsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private var channel: Channel? = null

    private val _uiState = MutableStateFlow(ChannelCommentsSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChannelCommentsSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun init(channelId: Long) {
        viewModelScope.launch {
            channelRepository.fetchById(channelId)
            channelRepository.getById(channelId).firstOrNull()?.let { loaded ->
                channel = loaded
                _uiState.update {
                    it.copy(
                        channelId = loaded.id,
                        commentsEnabled = loaded.commentsEnabled,
                        commentsRestrictedToSubscribers = loaded.commentsRestrictedToSubscribers
                    )
                }
            }
        }
    }

    fun changeCommentsEnabled(enabled: Boolean) {
        val currentChannel = channel ?: return
        val previous = _uiState.value.commentsEnabled

        _uiState.update {
            it.copy(commentsEnabled = enabled, isChangingCommentsEnabled = true)
        }

        viewModelScope.launch {
            channelRepository.setCommentsEnabled(currentChannel, enabled).onSuccess {
                channel = currentChannel.copy(commentsEnabled = enabled)
                _uiState.update { it.copy(isChangingCommentsEnabled = false) }
            }.onFailure {
                _uiState.update {
                    it.copy(commentsEnabled = previous, isChangingCommentsEnabled = false)
                }
                notifySaveFailed()
            }
        }
    }

    fun changeCommentsRestrictedToSubscribers(restricted: Boolean) {
        val currentChannel = channel ?: return
        val previous = _uiState.value.commentsRestrictedToSubscribers

        _uiState.update {
            it.copy(
                commentsRestrictedToSubscribers = restricted,
                isChangingCommentsRestricted = true
            )
        }

        viewModelScope.launch {
            channelRepository.setCommentsRestrictedToSubscribers(currentChannel, restricted)
                .onSuccess {
                    channel = currentChannel.copy(commentsRestrictedToSubscribers = restricted)
                    _uiState.update { it.copy(isChangingCommentsRestricted = false) }
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            commentsRestrictedToSubscribers = previous,
                            isChangingCommentsRestricted = false
                        )
                    }
                    notifySaveFailed()
                }
        }
    }

    fun deleteAllComments() {
        val channelId = _uiState.value.channelId
        if (channelId == -1L) return

        _uiState.update { it.copy(isDeletingAllComments = true) }

        viewModelScope.launch {
            channelRepository.deleteAllComments(channelId).onSuccess {
                _uiState.update { it.copy(isDeletingAllComments = false) }
                _uiEffect.emit(
                    ChannelCommentsSettingsEffect.ShowSnackbar(
                        UiText.StringResource(R.string.comments_deleted)
                    )
                )
            }.onFailure {
                _uiState.update { it.copy(isDeletingAllComments = false) }
                _uiEffect.emit(
                    ChannelCommentsSettingsEffect.ShowSnackbar(
                        UiText.StringResource(R.string.failed_to_save_changes)
                    )
                )
            }
        }
    }

    private suspend fun notifySaveFailed() {
        vibrationManager.vibrate(VibrationPattern.Error)
        _uiEffect.emit(
            ChannelCommentsSettingsEffect.ShowSnackbar(
                UiText.StringResource(R.string.failed_to_save_changes)
            )
        )
    }
}
