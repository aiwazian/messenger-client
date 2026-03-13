package com.aiwazian.messenger.utils

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

fun getFileExtension(fileName: String): String {
    return fileName.substringAfterLast('.', "").lowercase()
}

@SuppressLint("DefaultLocale")
fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = sizeInBytes.toDouble()
    var digitGroups = 0
    
    while (size >= 1024.0 && digitGroups < units.size - 1) {
        size /= 1024.0
        digitGroups++
    }
    
    return String.format("%.1f %s", size, units[digitGroups])
}

fun isFileExists(context: Context, filePath: String): Boolean {
    val appDir = context.getExternalFilesDir(null) ?: return false
    val file = File(appDir, filePath)
    return file.exists()
}
