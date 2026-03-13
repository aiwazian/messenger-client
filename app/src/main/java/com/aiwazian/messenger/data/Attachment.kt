package com.aiwazian.messenger.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Attachment(
    @Keep val id: Int,
    @Keep val messageId: Int,
    @Keep val name: String,
    @Keep val url: String,
    @Keep val size: Long
)
