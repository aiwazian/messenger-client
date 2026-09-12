/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.graphics.Color

object MediaCompressionConfig {
    
    const val PHOTO_MAX_DIMENSION = 1280
    
    const val PHOTO_JPEG_QUALITY = 85
    
    const val AVATAR_MAX_DIMENSION = 512
    
    const val AVATAR_JPEG_QUALITY = 90
    
    const val STICKER_SIZE = 512
    
    const val STICKER_WEBP_QUALITY = 90
    
    const val EMOJI_SIZE = 100
    
    const val EMOJI_WEBP_QUALITY = 90
    
    const val EMOJI_MIN_WEBP_QUALITY = 30
    
    const val EMOJI_MAX_SIZE_BYTES = 64L * 1024
    
    const val TRANSPARENCY_BACKGROUND_COLOR = Color.WHITE
    
    val KEEP_AS_IS_IMAGE_MIME_TYPES = setOf(
        "image/gif",
        "image/apng",
        "image/svg+xml"
    )
    
    val VIDEO_DEFAULT_QUALITY = VideoQuality.P720
    
    const val VIDEO_AUDIO_BITRATE = 128_000
}
