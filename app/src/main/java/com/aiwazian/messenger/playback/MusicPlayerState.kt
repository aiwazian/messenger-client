/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.net.Uri

enum class MusicRepeatMode {
    REPEAT_ALL,
    REPEAT_ONE
}

data class MusicPlayerState(
    val fileId: String? = null,
    val title: String = "",
    val artist: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

data class MusicTrack(
    val fileId: String,
    val uri: Uri,
    val title: String,
    val artist: String? = null,
    val artworkUri: Uri? = null,
    val artworkData: ByteArray? = null
)
