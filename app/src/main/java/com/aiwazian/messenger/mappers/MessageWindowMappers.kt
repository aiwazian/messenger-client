/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.domain.MessageSearchPage
import com.aiwazian.messenger.domain.MessagesPage
import com.aiwazian.messenger.network.dto.MessageSearchHitDto
import com.aiwazian.messenger.network.dto.MessageSearchResponseDto
import com.aiwazian.messenger.network.dto.MessagesWindowDto

fun MessagesWindowDto.toDomain() = MessagesPage(
    messages = messages.map { it.toDomain() },
    hasMoreBefore = hasMoreBefore,
    hasMoreAfter = hasMoreAfter,
    unreadCount = unreadCount,
    firstUnreadMessageId = firstUnreadMessageId
)

fun MessageSearchHitDto.toDomain() = MessageSearchHit(
    id = id,
    senderId = senderId,
    text = text,
    sendTime = sendTime
)

fun MessageSearchResponseDto.toDomain() = MessageSearchPage(
    items = items.map { it.toDomain() },
    nextCursorId = nextCursorId,
    scannedAll = scannedAll
)
