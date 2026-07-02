/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.network.dto.SearchResponseDto

fun SearchResponseDto.toDomain() = Search(
    chatId = chatId,
    name = name,
    username = username
)
