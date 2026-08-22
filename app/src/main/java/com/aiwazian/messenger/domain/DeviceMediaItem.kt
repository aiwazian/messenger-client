/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import android.net.Uri

/** Фото, видео или GIF из галереи устройства. */
data class DeviceMediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long,
    /** GIF в сетке стоит кадром, а вместо длительности у него своя плашка. */
    val isGif: Boolean = false
)
