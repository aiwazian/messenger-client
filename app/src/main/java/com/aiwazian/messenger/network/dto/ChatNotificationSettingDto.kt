/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Исключение по конкретному чату: уведомления включены или выключены независимо
 * от настройки его категории.
 *
 * Сервер отдаёт весь список целиком, а не флаг по одному чату: это заготовка под
 * будущий экран исключений, где нужны все принудительно включённые и выключенные чаты.
 */
@Serializable
data class ChatNotificationSettingDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("enabled") val enabled: Boolean
)

/** Тело запроса на добавление чата в исключения. */
@Serializable
data class UpdateChatNotificationSettingRequestDto(
    @SerialName("enabled") val enabled: Boolean
)
