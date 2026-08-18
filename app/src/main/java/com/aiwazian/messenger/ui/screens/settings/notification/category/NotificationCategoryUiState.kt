/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.category

import com.aiwazian.messenger.domain.Chat

/**
 * Экран одной категории уведомлений: список её исключений.
 *
 * isLoading — только до первой выдачи; дальше пусто значит именно «нет исключений»,
 * а не «ещё грузим».
 */
data class NotificationCategoryUiState(
    val isLoading: Boolean = true,
    val exceptions: List<NotificationExceptionItem> = emptyList(),
    val showDeleteAllDialog: Boolean = false
)

/**
 * Исключение вместе с чатом, к которому оно относится.
 *
 * chat может быть null, если чат ещё не подгрузился в список: карточку тогда
 * рисуем без имени и аватара, но с действием удаления — оно по chatId.
 */
data class NotificationExceptionItem(
    val chatId: Long,
    val chat: Chat?,
    val enabled: Boolean
)
