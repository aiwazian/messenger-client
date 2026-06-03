/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap

fun Uri.getFileName(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            cursor.getString(index)
        } else null
    }
}

fun Uri.getFileSize(context: Context): Long? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
            cursor.getLong(index)
        } else 0L
    }
}

fun Uri.getFileType(context: Context): String {
    context.contentResolver.getType(this)?.let { return it }
    
    val extension = lastPathSegment?.substringAfterLast('.')?.lowercase()
        ?: MimeTypeMap.getFileExtensionFromUrl(toString())?.lowercase()

    extension?.let {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)?.let { mimeType ->
            return mimeType
        }
    }
    
    return getMimeTypeFromMagicBytes(context)
}

private fun Uri.getMimeTypeFromMagicBytes(context: Context): String {
    if (scheme == "http" || scheme == "https") return "application/octet-stream"
    
    try {
        val inputStream = when (scheme) {
            "content" -> {
                context.contentResolver.openInputStream(this)
            }
            
            "file" -> {
                path?.let { java.io.FileInputStream(it) }
            }
            
            null -> {
                java.io.FileInputStream(toString())
            }
            
            else -> {
                path?.let { java.io.FileInputStream(it) }
            }
        }
        
        inputStream?.use { stream ->
            val bytes = ByteArray(12)
            val bytesRead = stream.read(bytes)
            
            if (bytesRead >= 8) {
                // JPEG
                if (
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() &&
                    bytes[2] == 0xFF.toByte()
                ) {
                    return "image/jpeg"
                }
                
                // PNG
                if (
                    bytes[0] == 0x89.toByte() &&
                    bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() &&
                    bytes[3] == 0x47.toByte()
                ) {
                    return "image/png"
                }
                
                // GIF
                if (
                    bytes[0] == 'G'.code.toByte() &&
                    bytes[1] == 'I'.code.toByte() &&
                    bytes[2] == 'F'.code.toByte()
                ) {
                    return "image/gif"
                }
                
                // MP4 / MOV
                if (
                    bytes[4] == 'f'.code.toByte() &&
                    bytes[5] == 't'.code.toByte() &&
                    bytes[6] == 'y'.code.toByte() &&
                    bytes[7] == 'p'.code.toByte()
                ) {
                    return "video/mp4"
                }
                
                // WEBP
                if (
                    bytesRead >= 12 &&
                    bytes[0] == 'R'.code.toByte() &&
                    bytes[1] == 'I'.code.toByte() &&
                    bytes[8] == 'W'.code.toByte() &&
                    bytes[9] == 'E'.code.toByte() &&
                    bytes[10] == 'B'.code.toByte() &&
                    bytes[11] == 'P'.code.toByte()
                ) {
                    return "image/webp"
                }
            }
        }
    } catch (e: Exception) {
        Log.e("UriExtensions", "getMimeTypeFromMagicBytes ${e.message}: ", e)
    }
    
    return "application/octet-stream"
}

fun String.getFolderNameFromMimeType(): String {
    return when {
        startsWith("image/", ignoreCase = true) -> "Images"
        startsWith("video/", ignoreCase = true) -> "Video"
        startsWith("text/", ignoreCase = true) ||
                startsWith("application/pdf", ignoreCase = true) ||
                startsWith("application/msword", ignoreCase = true) ||
                startsWith("application/vnd.", ignoreCase = true) -> "Documents"
        
        else -> "Other"
    }
}

fun Uri.getDuration(context: Context): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, this)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    } finally {
        retriever.release()
    }
}
