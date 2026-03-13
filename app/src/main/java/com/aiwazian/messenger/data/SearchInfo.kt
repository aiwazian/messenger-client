package com.aiwazian.messenger.data

import androidx.annotation.Keep

@Keep
data class SearchInfo(
    @Keep val chatId: Long,
    @Keep val name: String,
    @Keep val publicLink: String
)
