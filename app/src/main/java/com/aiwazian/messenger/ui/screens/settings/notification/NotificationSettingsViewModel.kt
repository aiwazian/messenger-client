/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.NotificationSettings
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    /**
     * Состояние читается из Room и кэша исключений, а не хранится в экране: так
     * переключатель сам подхватывает и событие с другого устройства, и откат при
     * ошибке запроса, а счётчик исключений — их удаление на экране категории.
     */
    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        notificationSettingsRepository.observe(),
        notificationSettingsRepository.observeChatExceptions()
    ) { settings, exceptions ->
        NotificationSettingsUiState(
            settings = settings,
            privateChatExceptions = exceptions.count { ChatFolderCategory.PRIVATE_CHATS.matches(it.chatId) },
            groupExceptions = exceptions.count { ChatFolderCategory.GROUPS.matches(it.chatId) },
            channelExceptions = exceptions.count { ChatFolderCategory.CHANNELS.matches(it.chatId) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSettingsUiState()
    )
    
    fun refresh() {
        viewModelScope.launch {
            notificationSettingsRepository.refresh()
            notificationSettingsRepository.refreshChatExceptions()
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
            notificationSettingsRepository.update(transform(uiState.value.settings)).onFailure { e ->
                Log.e(TAG, "Ошибка при обновлении настроек уведомлений", e)
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private companion object {
        const val TAG = "NotificationSettingsViewModel"
    }
}
