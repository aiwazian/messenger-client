/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class InviteLink(
    val id: Long,
    val code: String,
    val expiresAt: Long?,
    val maxUses: Int?,
    val uses: Int?
)
