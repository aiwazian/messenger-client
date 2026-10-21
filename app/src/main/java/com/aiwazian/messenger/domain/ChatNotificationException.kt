/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ChatNotificationException(
    val chatId: Long,
    val enabled: Boolean
)
