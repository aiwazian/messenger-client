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
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
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

/**
 * Пересобирает видео в MP4 меньшего разрешения перед отправкой.
 *
 * Метаданные снимать отдельно не нужно: дорожки кодируются заново, и в новый
 * контейнер попадает только то, что записал муксер. Геометка, модель камеры,
 * серийный номер и прочие атомы исходника до него не доходят.
 *
 * Заодно упрощается формат: MKV, AVI, HEVC, AV1 — всё уходит одинаковым H.264 в
 * MP4 с AAC. Так меньше вес и меньше поводов серверу и чужим клиентам не понять
 * файл.
 */
@OptIn(UnstableApi::class)
@Singleton
class VideoCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val videoMetadataReader: VideoMetadataReader
) {
    fun isCompressible(mimeType: String): Boolean {
        return mimeType.startsWith(VIDEO_MIME_PREFIX)
    }
    
    /**
     * Сжимает [source] до ступени [quality] и кладёт результат в [directory],
     * рядом с остальными копиями этой отправки.
     *
     * Возвращает null, если пересобрать не удалось или файл получился не меньше
     * исходного: в обоих случаях вызывающему выгоднее отправить оригинал, чем
     * копию тяжелее и хуже.
     */
    suspend fun compress(
        source: Uri,
        directory: File,
        quality: VideoQuality,
        name: String? = null
    ): Uri? {
        val metadata = videoMetadataReader.read(source)
        
        directory.mkdirs()
        
        val target = File(directory, mp4Name(name))
        
        // Готовая копия прошлой попытки: кодировать те же минуты второй раз незачем.
        if (target.exists() && target.length() > 0) {
            return Uri.fromFile(target)
        }
        
        val partial = File(directory, target.name + PARTIAL_SUFFIX)
        partial.delete()
        
        val isExported = try {
            export(source, partial, presentationFor(metadata, quality), quality)
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
        
        /*
         * Кодек иногда отдаёт файл тяжелее исходного: так бывает на коротком
         * видео, которое и так снято с низким битрейтом. Отправлять такое
         * вместо оригинала смысла нет.
         */
        val sourceSize = metadata?.sizeBytes ?: 0L
        if (sourceSize > 0 && compressedSize >= sourceSize) {
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
    
    /**
     * Во что масштабировать кадр.
     *
     * null означает «не масштабировать»: либо стороны неизвестны, либо кадр уже
     * мельче ступени, и растягивать его нельзя. Дорожки при этом всё равно
     * пересобираются, так что метаданные и сложный формат уходят и здесь.
     */
    private fun presentationFor(metadata: VideoMetadata?, quality: VideoQuality): Presentation? {
        val sourceShortSide = metadata?.shortSide ?: return null
        
        if (sourceShortSide <= 0 || sourceShortSide <= quality.shortSide) {
            return null
        }
        
        return Presentation.createForShortSide(quality.shortSide)
    }
    
    /**
     * Прогоняет видео через Transformer и ждёт результата.
     *
     * Transformer живёт на главном потоке: он привязывается к Looper того
     * потока, где его создали, и туда же приносит колбэки. Сама работа идёт в
     * кодеке, главный поток здесь только ждёт.
     */
    private suspend fun export(
        source: Uri,
        target: File,
        presentation: Presentation?,
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
                    if (presentation != null) {
                        setEffects(Effects(emptyList(), listOf(presentation)))
                    }
                }
                .build()
            
            transformer.start(editedMediaItem, target.absolutePath)
            
            /*
             * Отмену приносит тот поток, который отменил отправку, а Transformer
             * принимает вызовы только со своего, поэтому уносим её в главный.
             */
            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }
        }
    }
    
    /** Имя копии: расширение всегда mp4, потому что контейнер теперь всегда MP4. */
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
    }
}
