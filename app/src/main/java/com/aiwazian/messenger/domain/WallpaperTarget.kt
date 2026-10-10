/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface WallpaperTarget {
    @Serializable
    data object Global : WallpaperTarget

    @Serializable
    data class Chat(val chatId: Long) : WallpaperTarget

    @Serializable
    data class Channel(val channelId: Long) : WallpaperTarget

    @Serializable
    data class Group(val groupId: Long) : WallpaperTarget
}
