/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.SearchResponseDto
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.domain.SearchResultType

fun SearchResponseDto.toDomain(): Search = Search(
    type = if (this.type == "file") SearchResultType.FILE else SearchResultType.CHAT,
    chatId = this.chatId.toLongOrNull() ?: 0L,
    name = this.name,
    fileId = this.fileId,
    size = this.size?.toLongOrNull(),
    mimeType = this.mimeType,
    messageId = this.messageId?.toIntOrNull(),
    senderName = this.senderName,
    createdAt = this.createdAt?.toLongOrNull()
)
