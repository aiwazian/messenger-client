/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class EncodedEmoji(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

@Singleton
class EmojiEncoder @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    suspend fun encode(source: Uri): EncodedEmoji? = withContext(Dispatchers.IO) {
        val side = MediaCompressionConfig.EMOJI_SIZE
        
        try {
            val bounds = readBounds(source) ?: return@withContext null
            val decoded = decode(source, bounds, side) ?: return@withContext null
            
            val placed = placeIntoSquare(decoded, side)
            
            if (placed !== decoded) {
                decoded.recycle()
            }
            
            val directory = File(context.cacheDir, EMOJI_DIRECTORY_NAME)
            
            directory.mkdirs()
            dropStale(directory)
            
            val target = File(
                directory, "$NAME_PREFIX${System.currentTimeMillis()}.$WEBP_EXTENSION"
            )
            
            val size = compressIntoLimit(placed, target)
            
            placed.recycle()
            
            if (size <= 0 || size > MediaCompressionConfig.EMOJI_MAX_SIZE_BYTES) {
                target.delete()
                
                Log.w(TAG, "Encoded emoji does not fit the limit: $size bytes")
                
                return@withContext null
            }
            
            Log.i(TAG, "Encoded emoji ${target.name} into $size bytes")
            
            EncodedEmoji(
                uri = Uri.fromFile(target),
                name = target.name,
                size = size,
                mimeType = MIME_TYPE_WEBP
            )
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Not enough memory to encode an emoji", e)
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to encode an emoji", e)
            
            null
        }
    }
    
    private fun compressIntoLimit(source: Bitmap, target: File): Long {
        var quality = MediaCompressionConfig.EMOJI_WEBP_QUALITY
        
        while (true) {
            write(source, target, quality)
            
            val size = target.length()
            
            val fits = size <= MediaCompressionConfig.EMOJI_MAX_SIZE_BYTES
            val isLowest = quality <= MediaCompressionConfig.EMOJI_MIN_WEBP_QUALITY
            
            if (fits || isLowest) {
                return size
            }
            
            quality = maxOf(
                MediaCompressionConfig.EMOJI_MIN_WEBP_QUALITY,
                quality - QUALITY_STEP
            )
        }
    }
    
    private fun write(source: Bitmap, target: File, quality: Int) {
        FileOutputStream(target).use { stream ->
            val written = source.compress(
                Bitmap.CompressFormat.WEBP_LOSSY,
                quality,
                stream
            )
            
            if (!written) {
                throw IOException("WebP encoder refused ${target.name}")
            }
        }
    }
    
    private fun readBounds(source: Uri): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        
        context.contentResolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        
        return if (options.outWidth > 0 && options.outHeight > 0) options else null
    }
    
    private fun decode(source: Uri, bounds: BitmapFactory.Options, side: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, side)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        return context.contentResolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }
    
    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        
        while (minOf(width, height) / (sample * 2) >= target) {
            sample *= 2
        }
        
        return sample
    }
    
    private fun placeIntoSquare(source: Bitmap, side: Int): Bitmap {
        val longest = maxOf(source.width, source.height).toFloat()
        
        if (longest <= 0f) {
            return source
        }
        
        val scale = side / longest
        
        val width = source.width * scale
        val height = source.height * scale
        
        val result = createBitmap(side, side)
        
        val destination = RectF(
            (side - width) / 2f,
            (side - height) / 2f,
            (side + width) / 2f,
            (side + height) / 2f
        )
        
        Canvas(result).drawBitmap(
            source,
            null,
            destination,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        
        return result
    }
    
    private fun dropStale(directory: File) {
        val deadline = System.currentTimeMillis() - EMOJI_MAX_AGE_MS
        
        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) {
                file.delete()
            }
        }
    }
    
    companion object {
        const val MIME_TYPE_WEBP = "image/webp"
        const val WEBP_EXTENSION = "webp"
        
        private const val TAG = "EmojiEncoder"
        private const val EMOJI_DIRECTORY_NAME = "emoji_uploads"
        private const val NAME_PREFIX = "emoji_"
        private const val QUALITY_STEP = 10
        private const val EMOJI_MAX_AGE_MS = 6L * 60 * 60 * 1000
    }
}
