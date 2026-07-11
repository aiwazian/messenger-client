/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class PendingJoinRequest(
    val chatId: Long,
    val chatName: String,
    val createdAt: Long,
    val avatarFileId: String? = null
)
