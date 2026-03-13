package com.aiwazian.messenger.data

import kotlinx.serialization.Serializable

@Serializable
data class DeleteChatPayload (
    val chatId: Long
)