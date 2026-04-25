/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.enums.SearchResultType
import com.aiwazian.messenger.network.dto.SearchResponseDto

fun SearchResponseDto.toDomain(): Search = Search(
    type = if (type == "file") SearchResultType.FILE else SearchResultType.CHAT,
    chatId = chatId,
    name = name,
    fileId = fileId,
    size = size,
    mimeType = mimeType,
    messageId = messageId,
    senderName = senderName,
    createdAt = createdAt
)
