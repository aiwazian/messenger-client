/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.getFileType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apkInstaller: ApkInstaller
) {
    
    /**
     * Opens a file stored on the device.
     *
     * @param path absolute file system path, "file://" URI or "content://" URI.
     */
    suspend fun openFile(path: String) {
        try {
            val uri = path.toUri()
            
            if (uri.scheme.equals(SCHEME_CONTENT, ignoreCase = true)) {
                startViewIntent(uri, uri.getFileType(context))
                return
            }
            
            val file = resolveLocalFile(path)
            
            if (file == null || !file.exists()) {
                Log.e(TAG, "File does not exist at path: $path")
                showToast("File does not exist")
                return
            }
            
            if (file.extension.equals(EXTENSION_APK, ignoreCase = true)) {
                installApkFile(file)
            } else {
                openGenericFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in openFile", e)
            showToast("Error: ${e.message}")
        }
    }
    
    fun saveToGallery(path: String): Boolean {
        val file = resolveLocalFile(path)
        if (file == null || !file.exists()) return false
        
        val mimeType = file.toUri().getFileType(context)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            val appName = context.getString(R.string.app_name)
            val dir = if (mimeType.startsWith("video/")) {
                Environment.DIRECTORY_MOVIES
            } else {
                Environment.DIRECTORY_PICTURES
            }
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/$appName")
        }
        
        val collection = if (mimeType.startsWith("video/")) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        
        return try {
            val uri = context.contentResolver.insert(collection, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to gallery", e)
            false
        }
    }

    private suspend fun installApkFile(file: File) {
        when (apkInstaller.install(file)) {
            ApkInstallResult.STARTED -> Unit
            
            ApkInstallResult.PERMISSION_REQUIRED -> {
                showToast("Allow installing unknown apps to continue")
            }
            
            ApkInstallResult.UNTRUSTED_LOCATION, ApkInstallResult.FAILED -> {
                showToast("Cannot install this file")
            }
        }
    }
    
    private fun openGenericFile(file: File) {
        openFileWithFallback(file, file.toUri().getFileType(context))
    }
    
    private fun openFileWithFallback(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            startViewIntent(uri, mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file with fallback", e)
            showToast("Cannot open file: ${e.javaClass.simpleName}")
        }
    }
    
    private fun startViewIntent(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
        if (resolveInfo.isEmpty()) {
            intent.setDataAndType(uri, MIME_TYPE_ANY)
        }
        
        val chooserIntent =
            Intent.createChooser(intent, context.getString(R.string.app_name)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        
        context.startActivity(chooserIntent)
    }
    
    /**
     * Uploaded files are persisted as absolute paths and downloaded ones as
     * "file://" URIs, so both forms must resolve to the same file.
     */
    private fun resolveLocalFile(pathOrUri: String): File? {
        if (pathOrUri.isBlank()) return null
        if (pathOrUri.startsWith('/')) return File(pathOrUri)
        
        val uri = pathOrUri.toUri()
        val scheme = uri.scheme
        if (scheme != null && !scheme.equals(SCHEME_FILE, ignoreCase = true)) return null
        
        return uri.path?.let { File(it) }
    }
    
    fun saveToDownloads(path: String, displayName: String? = null): Boolean {
        val file = resolveLocalFile(path)
        if (file == null || !file.exists()) return false
        
        val mimeType = file.toUri().getFileType(context)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName ?: file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        
        return try {
            val uri = context.contentResolver.insert(collection, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to downloads", e)
            false
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private companion object {
        const val TAG = "FileHandler"
        const val SCHEME_FILE = "file"
        const val SCHEME_CONTENT = "content"
        const val EXTENSION_APK = "apk"
        const val MIME_TYPE_ANY = "*/*"
    }
}
