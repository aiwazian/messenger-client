/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import androidx.media3.common.MimeTypes

const val MIN_VIDEO_DURATION_MS = 100L
const val VIDEO_WEBM_MIME_TYPE = "video/webm"
const val VIDEO_MP4_MIME_TYPE = "video/mp4"
const val WEBM_FILE_EXTENSION = "webm"

enum class VideoExportTarget(
    val side: Int,
    val directoryName: String,
    val namePrefix: String,
    val extension: String,
    val mimeType: String,
    val videoMimeType: String,
    val videoBitrate: Int,
    val maxDurationMs: Long
) {
    STICKER(
        side = MediaCompressionConfig.STICKER_SIZE,
        directoryName = "video_stickers",
        namePrefix = "video_sticker_",
        extension = WEBM_FILE_EXTENSION,
        mimeType = VIDEO_WEBM_MIME_TYPE,
        videoMimeType = MimeTypes.VIDEO_VP8,
        videoBitrate = 1_000_000,
        maxDurationMs = 3_000L
    ),
    EMOJI(
        side = MediaCompressionConfig.EMOJI_SIZE,
        directoryName = "video_emojis",
        namePrefix = "video_emoji_",
        extension = WEBM_FILE_EXTENSION,
        mimeType = VIDEO_WEBM_MIME_TYPE,
        videoMimeType = MimeTypes.VIDEO_VP8,
        videoBitrate = 250_000,
        maxDurationMs = 3_000L
    ),
    AVATAR(
        side = MediaCompressionConfig.AVATAR_MAX_DIMENSION,
        directoryName = "video_avatars",
        namePrefix = "video_avatar_",
        extension = "mp4",
        mimeType = VIDEO_MP4_MIME_TYPE,
        videoMimeType = MimeTypes.VIDEO_H264,
        videoBitrate = 1_200_000,
        maxDurationMs = 10_000L
    )
}
