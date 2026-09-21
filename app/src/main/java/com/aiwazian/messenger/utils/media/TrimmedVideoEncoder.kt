/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class VideoCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val isFullFrame: Boolean
        get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f
}

data class EncodedVideo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

fun EncodedVideo.toEncodedSticker(): EncodedSticker = EncodedSticker(
    uri = uri,
    name = name,
    size = size,
    mimeType = mimeType
)

fun EncodedVideo.toEncodedEmoji(): EncodedEmoji = EncodedEmoji(
    uri = uri,
    name = name,
    size = size,
    mimeType = mimeType
)

@OptIn(UnstableApi::class)
@Singleton
class TrimmedVideoEncoder @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun encode(
        source: Uri,
        target: VideoExportTarget,
        startMs: Long,
        endMs: Long,
        transform: MediaTransform = MediaTransform.None,
        crop: VideoCropRect? = null
    ): EncodedVideo? = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, target.directoryName)

        directory.mkdirs()
        dropStale(directory)

        val name = "${target.namePrefix}${System.currentTimeMillis()}.${target.extension}"
        val result = File(directory, name)
        val partial = File(directory, "$name$PARTIAL_SUFFIX")

        partial.delete()

        val mediaItem = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(maxOf(endMs, startMs + 1L))
                    .build()
            )
            .build()

        val effects = videoEffects(target, transform, crop)

        val isExported = try {
            export(mediaItem, partial, target, effects)
        } catch (e: CancellationException) {
            partial.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unable to encode $source", e)
            false
        }

        if (!isExported || partial.length() <= 0L) {
            partial.delete()
            return@withContext null
        }

        if (!partial.renameTo(result)) {
            partial.delete()
            return@withContext null
        }

        EncodedVideo(
            uri = Uri.fromFile(result),
            name = name,
            size = result.length(),
            mimeType = target.mimeType
        )
    }

    private fun videoEffects(
        target: VideoExportTarget,
        transform: MediaTransform,
        crop: VideoCropRect?
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        if (transform.isMirrored) {
            effects += ScaleAndRotateTransformation.Builder()
                .setScale(transform.mirrorScaleX, 1f)
                .build()
        }

        if (transform.rotationDegrees != 0) {
            effects += ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(counterClockwiseDegrees(transform.rotationDegrees))
                .build()
        }

        if (crop != null && !crop.isFullFrame) {
            val leftNdc = ndc(crop.left)
            val rightNdc = ndc(crop.right)
            val topNdc = flippedNdc(crop.top)
            val bottomNdc = flippedNdc(crop.bottom)

            if (rightNdc > leftNdc && topNdc > bottomNdc) {
                effects += Crop(leftNdc, rightNdc, bottomNdc, topNdc)
            }
        }

        effects += Presentation.createForWidthAndHeight(
            target.side,
            target.side,
            Presentation.LAYOUT_SCALE_TO_FIT
        )

        return effects
    }

    private fun ndc(value: Float): Float = (value * 2f - 1f).coerceIn(-1f, 1f)

    private fun flippedNdc(fraction: Float): Float = (1f - fraction * 2f).coerceIn(-1f, 1f)

    private fun counterClockwiseDegrees(clockwiseDegrees: Int): Float =
        (FULL_TURN - clockwiseDegrees).mod(FULL_TURN).toFloat()

    private suspend fun export(
        mediaItem: MediaItem,
        target: File,
        exportTarget: VideoExportTarget,
        videoEffects: List<Effect>
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(exportTarget.videoBitrate)
                        .build()
                )
                .build()

            val builder = Transformer.Builder(context)
                .setVideoMimeType(exportTarget.videoMimeType)
                .setEncoderFactory(encoderFactory)

            if (exportTarget.extension == WEBM_EXTENSION) {
                builder.setMuxerFactory(WebmMuxerFactory())
            }

            val transformer = builder
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e(TAG, "Unable to export $mediaItem", exportException)

                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                })
                .build()

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(true)
                .apply {
                    if (videoEffects.isNotEmpty()) {
                        setEffects(Effects(emptyList(), videoEffects))
                    }
                }
                .build()

            transformer.start(editedMediaItem, target.absolutePath)

            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }
        }
    }

    private fun dropStale(directory: File) {
        val deadline = System.currentTimeMillis() - MAX_AGE_MS

        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) {
                file.delete()
            }
        }
    }

    private companion object {
        const val TAG = "TrimmedVideoEncoder"
        const val PARTIAL_SUFFIX = ".part"
        const val WEBM_EXTENSION = "webm"
        const val FULL_TURN = 360
        const val MAX_AGE_MS = 6L * 60 * 60 * 1000
    }
}
