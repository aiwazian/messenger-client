/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Пачка чатов для отметки прочитанными или непрочитанными. */
@Serializable
data class MarkChatsRequestDto(
    @SerialName("chatIds") val chatIds: List<String>
)
