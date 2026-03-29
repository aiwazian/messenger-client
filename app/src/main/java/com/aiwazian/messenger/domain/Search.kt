/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

enum class SearchResultType {
    CHAT, FILE
}

data class Search(
    val type: SearchResultType,
    val chatId: Long,
    val name: String,
    val fileId: String? = null,
    val size: Long? = null,
    val mimeType: String? = null,
    val messageId: Int? = null,
    val senderName: String? = null,
    val createdAt: Long? = null
)
