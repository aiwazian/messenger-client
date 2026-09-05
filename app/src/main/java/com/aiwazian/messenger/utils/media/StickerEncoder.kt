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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class EncodedSticker(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

@Singleton
class StickerEncoder @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    suspend fun encode(source: Uri): EncodedSticker? = withContext(Dispatchers.IO) {
        val side = MediaCompressionConfig.STICKER_SIZE
        
        try {
            val bounds = readBounds(source) ?: return@withContext null
            val decoded = decode(source, bounds, side) ?: return@withContext null
            
            val placed = placeIntoSquare(decoded, side)
            
            if (placed !== decoded) {
                decoded.recycle()
            }
            
            val directory = File(context.cacheDir, STICKER_DIRECTORY_NAME)
            
            directory.mkdirs()
            dropStale(directory)
            
            val target = File(
                directory, "$NAME_PREFIX${System.currentTimeMillis()}.$WEBP_EXTENSION"
            )
            
            FileOutputStream(target).use { stream ->
                val written = placed.compress(
                    Bitmap.CompressFormat.WEBP_LOSSY,
                    MediaCompressionConfig.STICKER_WEBP_QUALITY,
                    stream
                )
                
                if (!written) {
                    throw IOException("WebP encoder refused ${target.name}")
                }
            }
            
            placed.recycle()
            
            val size = target.length()
            
            if (size <= 0) {
                target.delete()
                
                return@withContext null
            }
            
            Log.i(TAG, "Encoded sticker ${target.name} into $size bytes")
            
            EncodedSticker(
                uri = Uri.fromFile(target),
                name = target.name,
                size = size,
                mimeType = MIME_TYPE_WEBP
            )
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Not enough memory to encode a sticker", e)
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to encode a sticker", e)
            
            null
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
        
        val result = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        
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
        val deadline = System.currentTimeMillis() - STICKER_MAX_AGE_MS
        
        directory.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) {
                file.delete()
            }
        }
    }
    
    companion object {
        const val MIME_TYPE_WEBP = "image/webp"
        const val WEBP_EXTENSION = "webp"
        
        private const val TAG = "StickerEncoder"
        private const val STICKER_DIRECTORY_NAME = "sticker_uploads"
        private const val NAME_PREFIX = "sticker_"
        private const val STICKER_MAX_AGE_MS = 6L * 60 * 60 * 1000
    }
}
