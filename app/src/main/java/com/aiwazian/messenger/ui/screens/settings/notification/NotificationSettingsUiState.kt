/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification

import com.aiwazian.messenger.domain.NotificationSettings

/**
 * Состояние экрана настроек уведомлений: сами переключатели категорий плюс
 * количество исключений в каждой — их показываем в supportingText.
 */
data class NotificationSettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val privateChatExceptions: Int = 0,
    val groupExceptions: Int = 0,
    val channelExceptions: Int = 0
)
