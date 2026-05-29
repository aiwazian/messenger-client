/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import android.content.Context
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
    
    val extension = when {
        scheme == "content" -> lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        else -> MimeTypeMap.getFileExtensionFromUrl(toString())?.lowercase()
    }
    
    extension?.let {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)?.let { mimeType ->
            return mimeType
        }
    }
    
    return getMimeTypeFromMagicBytes(context)
}

private fun Uri.getMimeTypeFromMagicBytes(context: Context): String {
    // Если это сетевая ссылка, мы не можем читать поток синхронно
    if (scheme == "http" || scheme == "https") return "application/octet-stream"
    
    try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val bytes = ByteArray(12)
            val bytesRead = inputStream.read(bytes, 0, 12)
            
            if (bytesRead >= 8) {
                // JPEG (начинается с FF D8 FF)
                if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                    return "image/jpeg"
                }
                // PNG (начинается с 89 50 4E 47)
                if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
                    return "image/png"
                }
                // GIF (начинается с GIF8)
                if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
                    return "image/gif"
                }
                // MP4 / MOV (байты с 4 по 7 содержат ASCII символы "ftyp")
                if (bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() && bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()) {
                    return "video/mp4"
                }
                // WEBP (начинается с RIFF, байты 8-11 содержат WEBP)
                if (bytesRead >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                    bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte()
                ) {
                    return "image/webp"
                }
            }
        }
    } catch (e: Exception) {
        Log.e(this::class.simpleName, "getMimeTypeFromMagicBytes ${e.message}: ", e)
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
