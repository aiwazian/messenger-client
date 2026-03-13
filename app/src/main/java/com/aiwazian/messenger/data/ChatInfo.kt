package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.enums.ChatType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Keep
@Serializable
data class ChatInfo(
    @Keep var id: Long = 0,
    @Keep var chatName: String = "",
    @Keep var isPinned: Boolean = false,
    @Keep var lastMessage: Message? = null
)
