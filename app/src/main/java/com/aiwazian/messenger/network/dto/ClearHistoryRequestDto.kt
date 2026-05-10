/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClearHistoryRequestDto(
    @SerialName("clearForRecipient")
    val clearForRecipient: Boolean = false
)
