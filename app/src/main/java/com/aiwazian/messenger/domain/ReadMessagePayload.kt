/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ReadMessagePayload(
    val chatId: Long,
    val messageId: Int
)
