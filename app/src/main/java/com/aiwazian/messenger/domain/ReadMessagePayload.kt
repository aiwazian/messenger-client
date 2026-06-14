/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class ReadMessagePayload(
    val chatId: Long,
    val messageId: Long,
    val userId: Long = 0,
    val time: Long = 0,
    val senderId: Long = 0,
    val sendTime: Long = 0
)
