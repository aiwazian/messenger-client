/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.utils.media.ImageCompressor
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import com.aiwazian.messenger.utils.media.MediaTransform
import com.aiwazian.messenger.utils.media.VideoCompressor
import com.aiwazian.messenger.utils.media.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentOutbox @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor,
    private val videoCompressor: VideoCompressor
) {

    suspend fun keep(
        uri: Uri,
        key: String,
        videoQuality: VideoQuality = MediaCompressionConfig.VIDEO_DEFAULT_QUALITY,
        transform: MediaTransform = MediaTransform.None,
        overlay: Bitmap? = null
    ): Uri = withContext(Dispatchers.IO) {
        if (directoryOf(uri) != null) {
            return@withContext uri
        }
        
        val name = uri.getFileName(context)?.replace('/', '_') ?: key
        val mimeType = uri.getFileType(context)
        
        if (imageCompressor.isCompressible(mimeType)) {
            val compressed = imageCompressor.compress(
                source = uri,
                directory = directoryFor(key),
                maxDimension = MediaCompressionConfig.PHOTO_MAX_DIMENSION,
                quality = MediaCompressionConfig.PHOTO_JPEG_QUALITY,
                name = name,
                transform = transform,
                overlay = overlay
            )
            
            if (compressed != null) {
                return@withContext compressed.uri
            }
            
            Log.w(TAG, "Unable to compress $uri, keeping it as is")
        }
    
        if (videoCompressor.isCompressible(mimeType)) {
            val compressed = videoCompressor.compress(
                source = uri,
                directory = directoryFor(key),
                quality = videoQuality,
                name = name,
                transform = transform,
                overlay = overlay
            )
            
            if (compressed != null) {
                return@withContext compressed
            }
            
            Log.w(TAG, "Unable to compress video $uri, keeping it as is")
        }
        
        if (uri.scheme == SCHEME_FILE) {
            return@withContext uri
        }
        
        val directory = directoryFor(key)
        directory.mkdirs()
        val target = File(directory, name)
        
        if (target.exists() && target.length() > 0) {
            return@withContext Uri.fromFile(target)
        }
        
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            
            if (copied) Uri.fromFile(target) else uri
        } catch (e: Exception) {
            Log.e(TAG, "Unable to keep $uri", e)
            target.delete()
            uri
        }
    }
    
    fun release(uri: Uri) {
        directoryOf(uri)?.deleteRecursively()
    }
  
    suspend fun cleanUp(keep: List<Uri>) = withContext(Dispatchers.IO) {
        val directories = File(context.filesDir, DIRECTORY_NAME).listFiles()
            ?: return@withContext
        
        val kept = keep.mapNotNull { directoryOf(it)?.absolutePath }.toSet()
        val threshold = System.currentTimeMillis() - MIN_AGE_MS
        
        directories.forEach { directory ->
            if (directory.absolutePath !in kept && directory.lastModified() < threshold) {
                directory.deleteRecursively()
            }
        }
    }
    
    private fun directoryFor(key: String): File {
        return File(File(context.filesDir, DIRECTORY_NAME), key)
    }
    
    private fun directoryOf(uri: Uri): File? {
        if (uri.scheme != SCHEME_FILE) {
            return null
        }
        
        val directory = File(uri.path ?: return null).parentFile ?: return null
        
        return if (directory.parentFile?.name == DIRECTORY_NAME) directory else null
    }
    
    private companion object {
        const val TAG = "AttachmentOutbox"
        const val SCHEME_FILE = "file"
        const val DIRECTORY_NAME = "outbox"
        
        const val MIN_AGE_MS = 60L * 60 * 1000
    }
}
