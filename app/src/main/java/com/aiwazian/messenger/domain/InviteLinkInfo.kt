/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class InviteLinkInfo(
    val channelId: Long,
    val channelName: String,
    val description: String?,
    val subscribersCount: Int
)
