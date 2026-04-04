/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class InviteLink(
    val id: Long,
    val chatId: Long,
    val code: String,
    val link: String,
    val expiresAt: String? = null,
    val maxUses: Int? = null,
    val uses: Int
)
