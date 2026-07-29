/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ChannelType

data class Channel(
    val id: Long,
    val ownerId: Long?,
    val name: String,
    val bio: String?,
    val subscribers: Int,
    val removedUsers: Int?,
    val channelType: ChannelType,
    val username: String?,
    val isSubscribed: Boolean,
    /**
     * Запрет копирования контента.
     *
     * Если true, то ни один участник, включая владельца, не может копировать
     * текст, пересылать сообщения и сохранять медиа.
     */
    val noCopy: Boolean = false,
    val avatars: List<Avatar> = emptyList()
)
