/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.net.Uri

data class VoiceQueueItem(
    val uri: Uri?,
    val fileId: String,
    val title: String,
    val subtitle: String? = null,
    val artworkUri: Uri?
)
