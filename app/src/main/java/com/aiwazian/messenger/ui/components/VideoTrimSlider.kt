/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import coil3.video.videoFramePercent

private val FRAME_TILE_WIDTH = 40.dp
private const val MAX_FRAME_COUNT = 30
private val TRACK_HEIGHT = 40.dp

@Composable
fun VideoTrimSlider(
    videoUri: Uri,
    durationMs: Long,
    state: RangeSliderState,
    minRangeMs: Long,
    maxRangeMs: Long,
    onRangeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var previousRange by remember(videoUri) { mutableStateOf(state.startValue..state.endValue) }
    var initializedUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(videoUri, durationMs) {
        if (durationMs <= 0L || initializedUri == videoUri) {
            return@LaunchedEffect
        }

        val maxGap = minOf(maxRangeMs, durationMs).toFloat() / durationMs

        state.startValue = 0f
        state.endValue = maxGap
        previousRange = 0f..maxGap
        initializedUri = videoUri
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val frameCount = (maxWidth / FRAME_TILE_WIDTH).toInt().coerceIn(1, MAX_FRAME_COUNT)
        val decoderFactory = remember { VideoFrameDecoder.Factory() }

        val frameRequests = remember(videoUri, frameCount, context) {
            List(frameCount) { index ->
                ImageRequest.Builder(context)
                    .data(videoUri)
                    .decoderFactory(decoderFactory)
                    .videoFramePercent((index + 0.5) / frameCount)
                    .build()
            }
        }

        RangeSlider(
            state = state,
            onValueChange = { raw ->
                if (durationMs <= 0L) {
                    previousRange = raw

                    return@RangeSlider
                }

                val minGap = minOf(minRangeMs, durationMs).toFloat() / durationMs
                val maxGap = minOf(maxRangeMs, durationMs).toFloat() / durationMs
                val endUnchanged = raw.endInclusive == previousRange.endInclusive

                val clamped = if (endUnchanged) {
                    coerceTrimStart(
                        start = raw.start,
                        fixedEnd = previousRange.endInclusive,
                        minGap = minGap,
                        maxGap = maxGap
                    )..previousRange.endInclusive
                } else {
                    previousRange.start..coerceTrimEnd(
                        end = raw.endInclusive,
                        fixedStart = previousRange.start,
                        minGap = minGap,
                        maxGap = maxGap
                    )
                }

                state.startValue = clamped.start
                state.endValue = clamped.endInclusive
                previousRange = clamped
            },
            onValueChangeFinished = onRangeChangeFinished,
            track = { rangeState ->
                val startFraction = positionFraction(rangeState.startValue, rangeState.trackRange)
                val endFraction = positionFraction(rangeState.endValue, rangeState.trackRange)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TRACK_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        frameRequests.forEach { request ->
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(startFraction)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(1f - endFraction)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }
            }
        )
    }
}

private fun coerceTrimStart(
    start: Float,
    fixedEnd: Float,
    minGap: Float,
    maxGap: Float,
): Float {
    val lower = (fixedEnd - maxGap).coerceAtLeast(0f)
    val upper = (fixedEnd - minGap).coerceAtLeast(0f)

    return start.coerceIn(lower, upper)
}

private fun coerceTrimEnd(
    end: Float,
    fixedStart: Float,
    minGap: Float,
    maxGap: Float,
): Float {
    val lower = (fixedStart + minGap).coerceAtMost(1f)
    val upper = (fixedStart + maxGap).coerceAtMost(1f)

    return end.coerceIn(lower, upper)
}

private fun positionFraction(
    value: Float,
    trackRange: ClosedFloatingPointRange<Float>,
): Float {
    val delta = trackRange.endInclusive - trackRange.start
    val rawFraction = if (delta == 0f) 0f else (value - trackRange.start) / delta

    return rawFraction.coerceIn(0f, 1f)
}
