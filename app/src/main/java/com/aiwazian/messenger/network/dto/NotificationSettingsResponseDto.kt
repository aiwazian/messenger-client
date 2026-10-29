/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.domain.NotificationSettings
import kotlinx.serialization.Serializable

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
