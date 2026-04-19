/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.common.utils.UiText

data class Chat(
    val id: Long,
    val chatName: UiText,
    val isPinned: Boolean,
    val lastMessage: Message?
)
