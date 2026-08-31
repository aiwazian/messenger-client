/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Что известно о видео до сжатия. */
data class VideoMetadata(
    /** Ширина кадра с учётом поворота, в пикселях. */
    val width: Int,
    /** Высота кадра с учётом поворота, в пикселях. */
    val height: Int,
    val durationMs: Long,
    val sizeBytes: Long
) {
    /** Меньшая сторона кадра: по ней считаются доступные ступени сжатия. */
    val shortSide: Int
        get() = minOf(width, height)
}

/**
 * Читает стороны кадра, длительность и вес видео.
 *
 * Отдельно от DeviceMediaRepository, потому что видео приходит не только из
 * галереи: то же самое нужно знать про файл из системного выбора и про то, что
 * прилетело через «Поделиться», а MediaStore о них ничего не расскажет.
 */
@Singleton
class VideoMetadataReader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun read(uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, uri)
            
            val width = retriever.readInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.readInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            
            if (width <= 0 || height <= 0) {
                return@withContext null
            }
            
            /*
             * Портретное видео лежит в контейнере горизонтальным кадром и
             * флагом поворота. Без обмена сторон короткая сторона нашлась бы по
             * 1920, и слайдер предложил бы разрешения выше настоящего.
             */
            val rotation = retriever.readInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val isSwapped = rotation == ROTATION_QUARTER || rotation == ROTATION_THREE_QUARTERS
            
            VideoMetadata(
                width = if (isSwapped) height else width,
                height = if (isSwapped) width else height,
                durationMs = retriever.readLong(MediaMetadataRetriever.METADATA_KEY_DURATION),
                sizeBytes = uri.getFileSize(context) ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read metadata of $uri", e)
            null
        } finally {
            retriever.release()
        }
    }
    
    private fun MediaMetadataRetriever.readInt(key: Int): Int {
        return extractMetadata(key)?.toIntOrNull() ?: 0
    }
    
    private fun MediaMetadataRetriever.readLong(key: Int): Long {
        return extractMetadata(key)?.toLongOrNull() ?: 0L
    }
    
    private companion object {
        const val TAG = "VideoMetadataReader"
        const val ROTATION_QUARTER = 90
        const val ROTATION_THREE_QUARTERS = 270
    }
}
