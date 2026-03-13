package com.aiwazian.messenger.data

import androidx.annotation.Keep

@Keep
data class FolderInfo(
    @Keep var id: Int = 0,
    @Keep var name: String = "",
    @Keep var chats: List<ChatInfo> = emptyList()
)
