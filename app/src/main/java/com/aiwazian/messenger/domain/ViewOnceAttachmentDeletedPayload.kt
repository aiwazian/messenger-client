/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class ViewOnceAttachmentDeletedPayload(
    val chatId: Long,
    val messageId: Long,
    val fileId: String,
    val messageDeleted: Boolean = false
)
