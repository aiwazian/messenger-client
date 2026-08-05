/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
    
    suspend fun openFile(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
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
        val file = File(path)
        if (!file.exists()) return false
        
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
            ApkInstallResult.PERMISSION_REQUIRED -> showToast("Allow installing unknown apps to continue")
            ApkInstallResult.UNTRUSTED_LOCATION -> showToast("Cannot install this file")
            ApkInstallResult.FAILED -> showToast("Cannot install this file")
        }
    }
    
    private fun openGenericFile(file: File) {
        openFileWithFallback(file, file.toUri().getFileType(context))
    }
    
    private fun openFileWithFallback(file: File, mimeType: String) {
