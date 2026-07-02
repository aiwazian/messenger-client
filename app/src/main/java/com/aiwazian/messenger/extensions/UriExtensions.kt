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
import java.io.FileInputStream

fun Uri.getFileName(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            cursor.getString(index)
        } else null
    }
}

fun Uri.getFileSize(context: Context): Long? {
    when (scheme) {
        "file" -> {
            val path = path ?: return null
            val file = java.io.File(path)
            return if (file.exists()) file.length() else null
        }
        
        "content" -> {
            return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
                    val size = cursor.getLong(index)
                    if (size > 0) size else null
                } else null
            }
        }
        
        else -> return null
    }
}

fun Uri.getFileType(context: Context): String {
    val extension = lastPathSegment?.let { segment ->
        if (segment.contains('.')) segment.substringAfterLast('.').lowercase() else null
    } ?: MimeTypeMap.getFileExtensionFromUrl(toString())?.lowercase()
    
    var mimeType: String? = null
    
    if (extension != null) {
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mimeType == null) {
            mimeType = when (extension) {
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "apk" -> "application/vnd.android.package-archive"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "xls" -> "application/vnd.ms-excel"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "csv" -> "text/csv"
                "rtf" -> "application/rtf"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "tar" -> "application/x-tar"
                "gz" -> "application/gzip"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/x-wav"
                "m4a" -> "audio/mp4"
                "mp4" -> "video/mp4"
                "avi" -> "video/x-msvideo"
                "mkv" -> "video/x-matroska"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                "html", "htm" -> "text/html"
                "xml" -> "text/xml"
                "json" -> "application/json"
                else -> null
            }
        }
    }
    
    if (mimeType != null && mimeType != "application/octet-stream") {
        return mimeType
    }
    
    val resolverType = context.contentResolver.getType(this)
    if (resolverType != null && resolverType != "application/octet-stream") {
        return resolverType
    }
    
    val magicType = getMimeTypeFromMagicBytes(context)
    if (magicType != "application/octet-stream") {
        return magicType
    }
    
    return resolverType ?: "application/octet-stream"
}

private fun Uri.getMimeTypeFromMagicBytes(context: Context): String {
    if (scheme !in listOf("content", "file")) {
        return "application/octet-stream"
    }
    
    try {
        val inputStream = when (scheme) {
            "content" -> {
                context.contentResolver.openInputStream(this)
            }
            
            "file" -> {
                path?.let { FileInputStream(it) }
            }
            
            null -> {
                FileInputStream(toString())
            }
            
            else -> {
                path?.let { FileInputStream(it) }
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
