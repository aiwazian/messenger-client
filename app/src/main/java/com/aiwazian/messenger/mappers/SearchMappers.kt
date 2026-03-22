/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.SearchResponseDto
import com.aiwazian.messenger.domain.Search

fun SearchResponseDto.toDomain(): Search = Search(
    chatId = this.chatId,
    name = this.name
)
