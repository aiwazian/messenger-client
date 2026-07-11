/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PendingJoinRequestDto(
    val chatId: String,
    val chatName: String,
    val createdAt: String,
    val avatarFileId: String? = null
)
