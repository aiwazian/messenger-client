/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.NotificationSettings
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    /**
     * Состояние читается из Room, а не хранится в экране: так переключатель сам
     * подхватывает и событие с другого устройства, и откат при ошибке запроса.
     */
    val uiState: StateFlow<NotificationSettings> = notificationSettingsRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings()
        )
    
    fun refresh() {
        viewModelScope.launch {
            notificationSettingsRepository.refresh()
        }
    }
    
    fun togglePrivateChats() = update { it.copy(privateChats = !it.privateChats) }
    
    fun toggleGroups() = update { it.copy(groups = !it.groups) }
    
    fun toggleChannels() = update { it.copy(channels = !it.channels) }
    
    /**
     * Каждое переключение сразу уходит на сервер — кнопки «Сохранить» здесь нет.
     * О неудаче говорит вибрация и вернувшийся в исходное положение переключатель.
     */
    private fun update(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch {
            notificationSettingsRepository.update(transform(uiState.value)).onFailure { e ->
                Log.e(TAG, "Ошибка при обновлении настроек уведомлений", e)
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private companion object {
        const val TAG = "NotificationSettingsViewModel"
    }
}
