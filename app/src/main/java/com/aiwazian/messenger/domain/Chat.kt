/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Long = 0,
    val chatName: String = "",
    val isPinned: Boolean = false,
    val lastMessage: Message? = null
)
