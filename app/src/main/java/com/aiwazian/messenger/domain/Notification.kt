/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Notification(
    val chatId: Long,
    val title: String,
    val message: String
)
