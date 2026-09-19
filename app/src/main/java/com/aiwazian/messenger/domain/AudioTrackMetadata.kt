/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class AudioTrackMetadata(
    val title: String,
    val artist: String? = null,
    val durationMs: Int = 0,
    val cover: ByteArray? = null
)
