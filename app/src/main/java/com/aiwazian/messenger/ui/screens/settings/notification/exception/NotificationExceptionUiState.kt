/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception

import com.aiwazian.messenger.domain.Chat

/**
 * Состояние экрана настройки одного исключения.
 *
 * chat нужен только для заголовка, поэтому null — не ошибка: чат мог ещё не
 * подгрузиться в список, а переключатель работает по chatId и без него.
 *
 * hasException отделён от notificationsEnabled: пока исключения нет, переключатель
 * показывает состояние по категории, а удалять ещё нечего.
 */
data class NotificationExceptionUiState(
    val chat: Chat? = null,
    val notificationsEnabled: Boolean = true,
    val hasException: Boolean = false
)
