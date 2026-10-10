/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Wallpaper(
    val fileId: String,
    val url: String,
    val darken: Int,
    val blur: Int,
    val offsetX: Float,
    val offsetY: Float,
    val source: WallpaperSource
)

enum class WallpaperSource {
    USER,
    CHAT,
    CHANNEL,
    GROUP
}
