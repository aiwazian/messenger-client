/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.content.Context
import android.net.Uri
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.aiwazian.messenger.extensions.getFileType

fun avatarImageRequest(context: Context, uri: Uri?): ImageRequest =
    ImageRequest.Builder(context)
        .data(uri)
        .apply {
            if (uri != null && uri.getFileType(context).startsWith("video/")) {
                decoderFactory(VideoFrameDecoder.Factory())
            }
        }
        .build()
