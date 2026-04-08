/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChatType

data class InviteLinkInfo(
    val chatId: Long,
    val name: String,
    val description: String?,
    val membersCount: Int,
    val isBanned: Boolean,
    val isJoined: Boolean,
    val type: ChatType
)
