/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class DeleteChatPayload (
    val chatId: Long
)
