/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Уведомления одного чата: пункт в меню «три точки» и колокольчик рядом с названием.
 *
 * Живёт отдельно от ChatViewModel: тот ведает всем содержимым чата, а здесь одна
 * настройка, которая тем же способом понадобится будущему экрану исключений.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatNotificationsViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository
) : ViewModel() {
    
    private val chatId = MutableStateFlow<Long?>(null)
    
    /** Итоговое состояние чата: сюда смотрят и колокольчик, и надпись в меню. */
    val isMuted: StateFlow<Boolean> = chatId
        .filterNotNull()
        .flatMapLatest { notificationSettingsRepository.observeChatMuted(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT), false)
    
    private val _snackbar = Channel<UiText>(Channel.BUFFERED)
    
    /** Подтверждение внизу экрана: что именно произошло с уведомлениями. */
    val snackbar: Flow<UiText> = _snackbar.receiveAsFlow()
    
    fun bind(chatId: Long) {
        this.chatId.value = chatId
    }
    
    /**
     * Переключить уведомления по чату: запрос уходит сразу, без диалогов и подтверждений.
     *
     * Snackbar показывается только после успешного ответа: обещать тишину, которой
     * не случилось, хуже, чем промолчать.
     */
    fun toggle() {
        val id = chatId.value ?: return
        val enabled = isMuted.value
        
        viewModelScope.launch {
            notificationSettingsRepository.setChatNotifications(id, enabled).onSuccess {
                _snackbar.send(
                    UiText.StringResource(
                        if (enabled) R.string.chat_notifications_enabled
                        else R.string.chat_notifications_disabled
                    )
                )
            }
        }
    }
    
    private companion object {
        /** Сколько держать подписку на Room после ухода экрана в фон. */
        const val SUBSCRIPTION_TIMEOUT = 5_000L
    }
}
