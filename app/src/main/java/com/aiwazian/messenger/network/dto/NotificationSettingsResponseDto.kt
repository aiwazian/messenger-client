/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.domain.NotificationSettings
import kotlinx.serialization.Serializable

/**
 * Ответ сервера и пейлоад события settings:notifications — формат у них общий.
 *
 * Значения по умолчанию стоят на случай, если сервер пришлёт частичный объект.
 */
@Serializable
data class NotificationSettingsResponseDto(
    val privateChats: Boolean = true,
    val groups: Boolean = true,
    val channels: Boolean = true
) {
    fun toDomain() = NotificationSettings(
        privateChats = privateChats,
        groups = groups,
        channels = channels
    )
}
