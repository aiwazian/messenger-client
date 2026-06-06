/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback


data class VoicePlayerState(
    val currentFileId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)
