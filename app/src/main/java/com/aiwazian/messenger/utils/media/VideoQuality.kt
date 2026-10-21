/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import kotlin.math.roundToInt

enum class VideoQuality(
    val shortSide: Int,
    val videoBitrate: Int
) {
    P360(360, 800_000),
    P480(480, 1_200_000),
    P720(720, 2_500_000),
    P1080(1080, 4_500_000);
    
    val label: String
        get() = "${shortSide}p"
    
    companion object {
        val DEFAULT = P720

        fun availableFor(shortSide: Int): List<VideoQuality> {
            if (shortSide <= 0) {
                return entries
            }
            
            return entries.filter { it.shortSide <= shortSide }
        }
    }
}

data class VideoFrame(
    val width: Int,
    val height: Int
)

fun VideoQuality.frameFor(width: Int, height: Int): VideoFrame {
    if (width <= 0 || height <= 0) {
        return VideoFrame(width, height)
    }
    
    val sourceShortSide = minOf(width, height)
    
    if (sourceShortSide <= shortSide) {
        return VideoFrame(width, height)
    }
    
    val scale = shortSide.toDouble() / sourceShortSide
    
    return VideoFrame(
        width = toEvenSide((width * scale).roundToInt()),
        height = toEvenSide((height * scale).roundToInt())
    )
}

fun VideoQuality.estimateSizeBytes(durationMs: Long, sourceSizeBytes: Long): Long {
    if (durationMs <= 0) {
        return sourceSizeBytes
    }
    
    val bitrate = videoBitrate.toLong() + MediaCompressionConfig.VIDEO_AUDIO_BITRATE
    val estimated = bitrate * durationMs / (BITS_IN_BYTE * MILLIS_IN_SECOND)
    
    return if (sourceSizeBytes > 0) minOf(estimated, sourceSizeBytes) else estimated
}

private fun toEvenSide(value: Int): Int = value - value % 2

private const val BITS_IN_BYTE = 8
private const val MILLIS_IN_SECOND = 1000
