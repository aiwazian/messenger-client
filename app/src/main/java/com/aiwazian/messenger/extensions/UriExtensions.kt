/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
    val type = context.contentResolver.getType(this)
    if (type != null) return type
    
    val extension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
    if (extension != null) {
        val mimeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        if (mimeFromExt != null) return mimeFromExt
    }
    
    return "application/octet-stream"
}
