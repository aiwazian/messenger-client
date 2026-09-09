/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.media.ExifInterface
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class ImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    data class CompressedImage(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String = MIME_TYPE_JPEG
    )
    
    fun isCompressible(mimeType: String): Boolean {
        if (!mimeType.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true)) {
            return false
        }
        
        return mimeType.lowercase() !in MediaCompressionConfig.KEEP_AS_IS_IMAGE_MIME_TYPES
    }
    
    suspend fun compress(
        source: Uri,
        directory: File,
        maxDimension: Int,
        quality: Int,
        name: String? = null,
        transform: MediaTransform = MediaTransform.None
    ): CompressedImage? = withContext(Dispatchers.IO) {
        val fileName = renamed(name ?: source.getFileName(context), JPEG_EXTENSION)
        val target = File(directory, fileName)
        
        if (target.exists() && target.length() > 0) {
            return@withContext CompressedImage(
                uri = Uri.fromFile(target),
                name = fileName,
                size = target.length()
            )
        }
        
        try {
            val bounds = readBounds(source) ?: return@withContext null
            val decoded = decode(source, bounds, maxDimension) ?: return@withContext null
            
            val prepared = try {
                prepare(decoded, readOrientation(source), maxDimension, transform)
            } catch (e: Exception) {
                decoded.recycle()
                throw e
            }
            
            try {
                write(prepared, target, quality)
            } finally {
                if (prepared !== decoded) {
                    prepared.recycle()
                }
                
                decoded.recycle()
            }
            
            val size = target.length()
            
            if (size <= 0) {
                target.delete()
                return@withContext null
            }
            
            Log.i(TAG, "Compressed $fileName into $size bytes")
            
            CompressedImage(uri = Uri.fromFile(target), name = fileName, size = size)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Not enough memory to compress $source", e)
            target.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unable to compress $source", e)
            target.delete()
            null
        }
    }
    
    suspend fun compressAvatar(source: Uri): Uri? = withContext(Dispatchers.IO) {
        if (!isCompressible(source.getFileType(context))) {
            return@withContext null
        }
        
        val root = File(context.cacheDir, AVATAR_DIRECTORY_NAME)
        dropStale(root)
        
        val directory = File(root, System.currentTimeMillis().toString())
        
        compress(
            source = source,
            directory = directory,
            maxDimension = MediaCompressionConfig.AVATAR_MAX_DIMENSION,
            quality = MediaCompressionConfig.AVATAR_JPEG_QUALITY,
            name = source.getFileName(context) ?: DEFAULT_AVATAR_NAME
        )?.uri
    }
    
    private fun readBounds(source: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = openStream(source) ?: return null
        
        stream.use { BitmapFactory.decodeStream(it, null, options) }
        
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }
        
        return options.outWidth to options.outHeight
    }
    
    private fun decode(source: Uri, bounds: Pair<Int, Int>, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.first, bounds.second, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        val stream = openStream(source) ?: return null
        
        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }
    
    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        val longest = max(width, height)
        
        while (longest / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        
        return sample
    }
    
    private fun prepare(
        source: Bitmap,
        orientation: Int,
        maxDimension: Int,
        transform: MediaTransform
    ): Bitmap {
        val longest = max(source.width, source.height)
        val scale = if (longest > maxDimension) maxDimension.toFloat() / longest else 1f
        val matrix = orientationMatrix(orientation)
        
        if (transform.isMirrored) {
            matrix.postScale(-1f, 1f)
        }
        
        matrix.postRotate(transform.rotationDegrees.toFloat())
        
        if (matrix.isIdentity && scale == 1f && !source.hasAlpha()) {
            return source
        }
        
        matrix.postScale(scale, scale)
        
        val frame = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        matrix.mapRect(frame)
        matrix.postTranslate(-frame.left, -frame.top)
        
        val width = frame.width().roundToInt().coerceAtLeast(1)
        val height = frame.height().roundToInt().coerceAtLeast(1)
        
        val result = createBitmap(width, height)
        val canvas = Canvas(result)
        
        if (source.hasAlpha()) {
            canvas.drawColor(MediaCompressionConfig.TRANSPARENCY_BACKGROUND_COLOR)
        }
        
        canvas.drawBitmap(
            source,
            matrix,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        
        return result
    }
    
    private fun orientationMatrix(orientation: Int): Matrix {
        val matrix = Matrix()
        
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        
        return matrix
    }
    
    private fun readOrientation(source: Uri): Int {
        return try {
            val stream = openStream(source) ?: return ExifInterface.ORIENTATION_NORMAL
            
            stream.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read orientation of $source: ${e.message}")
            ExifInterface.ORIENTATION_NORMAL
        }
    }
    
    private fun write(
        bitmap: Bitmap,
        target: File,
        quality: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ) {
        target.parentFile?.mkdirs()
        
        val partial = File(target.parentFile, "${target.name}$PARTIAL_SUFFIX")
        
        try {
            partial.outputStream().use { output ->
                if (!bitmap.compress(format, quality, output)) {
                    throw IOException("${format.name} encoder refused ${target.name}")
                }
            }
            
            if (!partial.renameTo(target)) {
                throw IOException("Unable to move ${partial.name} to ${target.name}")
            }
        } finally {
            partial.delete()
        }
    }
    
    private fun openStream(source: Uri): InputStream? {
        return try {
            if (source.scheme == SCHEME_FILE) {
                source.path?.let { FileInputStream(it) }
            } else {
                context.contentResolver.openInputStream(source)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open $source: ${e.message}")
            null
        }
    }
    
    private fun renamed(name: String?, extension: String): String {
        val safe = name?.replace('/', '_')?.trim().orEmpty()
        val base = safe.substringBeforeLast('.', safe).ifBlank { DEFAULT_NAME }
        
        return "$base.$extension"
    }
    
    private fun dropStale(root: File) {
        val threshold = System.currentTimeMillis() - PENDING_MAX_AGE_MS
        
        root.listFiles()?.forEach { entry ->
            if (entry.lastModified() < threshold) {
                entry.deleteRecursively()
            }
        }
    }
    
    companion object {
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val JPEG_EXTENSION = "jpg"
        
        private const val TAG = "ImageCompressor"
        private const val IMAGE_MIME_PREFIX = "image/"
        private const val SCHEME_FILE = "file"
        private const val PARTIAL_SUFFIX = ".part"
        private const val DEFAULT_NAME = "image"
        private const val DEFAULT_AVATAR_NAME = "avatar"
        private const val AVATAR_DIRECTORY_NAME = "avatar_uploads"
        private const val PENDING_MAX_AGE_MS = 60L * 60 * 1000
    }
}
