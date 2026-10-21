/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateNotificationSettingsRequestDto(
    val privateChats: Boolean,
    val groups: Boolean,
    val channels: Boolean
)
