/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
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

@OptIn(UnstableApi::class)
@Singleton
class VideoCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val videoMetadataReader: VideoMetadataReader
) {
    fun isCompressible(mimeType: String): Boolean {
        return mimeType.startsWith(VIDEO_MIME_PREFIX)
    }

    suspend fun compress(
        source: Uri,
        directory: File,
        quality: VideoQuality,
        name: String? = null,
        transform: MediaTransform = MediaTransform.None,
        overlay: Bitmap? = null
    ): Uri? {
        val metadata = videoMetadataReader.read(source)
        
        directory.mkdirs()
        
        val target = File(directory, mp4Name(name))
        
        if (target.exists() && target.length() > 0) {
            return Uri.fromFile(target)
        }
        
        val partial = File(directory, target.name + PARTIAL_SUFFIX)
        partial.delete()
        
        val effects = videoEffects(presentationFor(metadata, quality), transform, overlay)
        
        val isExported = try {
            export(source, partial, effects, quality)
        } catch (e: CancellationException) {
            partial.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unable to compress $source", e)
            false
        }
        
        val compressedSize = partial.length()
        
        if (!isExported || compressedSize <= 0) {
            partial.delete()
            return null
        }
        
        val sourceSize = metadata?.sizeBytes ?: 0L
        val isEdited = !transform.isIdentity || overlay != null
        
        if (!isEdited && sourceSize > 0 && compressedSize >= sourceSize) {
            Log.i(TAG, "Compressed $source is not smaller than the source, keeping the source")
            partial.delete()
            return null
        }
        
        if (!partial.renameTo(target)) {
            partial.delete()
            return null
        }
        
        return Uri.fromFile(target)
    }

    private fun videoEffects(
        presentation: Presentation?,
        transform: MediaTransform,
        overlay: Bitmap?
    ): List<Effect> {
        val effects = mutableListOf<Effect>()
        
        if (overlay != null) {
            effects += OverlayEffect(listOf(BitmapOverlay.createStaticBitmapOverlay(overlay)))
        }
        
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
        
        if (presentation != null) {
            effects += presentation
        }
        
        return effects
    }
 
    private fun counterClockwiseDegrees(clockwiseDegrees: Int): Float =
        (FULL_TURN - clockwiseDegrees).mod(FULL_TURN).toFloat()

    private fun presentationFor(metadata: VideoMetadata?, quality: VideoQuality): Presentation? {
        val sourceShortSide = metadata?.shortSide ?: return null
        
        if (sourceShortSide <= 0 || sourceShortSide <= quality.shortSide) {
            return null
        }
        
        return Presentation.createForShortSide(quality.shortSide)
    }

    private suspend fun export(
        source: Uri,
        target: File,
        videoEffects: List<Effect>,
        quality: VideoQuality
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(quality.videoBitrate)
                        .build()
                )
                .build()
            
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
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
                        Log.e(TAG, "Unable to export $source", exportException)
                        
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                })
                .build()
            
            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(source))
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
    
    private fun mp4Name(name: String?): String {
        val base = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
        
        return "$base.$MP4_EXTENSION"
    }
    
    private companion object {
        const val TAG = "VideoCompressor"
        const val VIDEO_MIME_PREFIX = "video/"
        const val MP4_EXTENSION = "mp4"
        const val PARTIAL_SUFFIX = ".part"
        const val DEFAULT_NAME = "video"
        const val FULL_TURN = 360
    }
}
