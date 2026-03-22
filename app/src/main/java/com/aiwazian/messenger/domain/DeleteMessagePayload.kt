/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class DeleteMessagePayload(
    val chatId: Long,
    val messageId: Int
)
