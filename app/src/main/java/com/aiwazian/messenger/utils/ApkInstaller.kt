/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands an apk file over to the system installer.
 *
 * The installer draws the confirmation, the progress and the result of an
 * installation and asks for the "install unknown apps" permission when it is
 * missing, so the application does not need a screen of its own.
 *
 * Only files inside the application storage are accepted: a file in shared
 * storage can be replaced by another application between the moment it is
 * checked and the moment it is installed. The file is shared with the installer
 * through a [FileProvider] instead of a "file://" uri.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    /**
     * Opens the system installer for [file].
     *
     * @return false when the file is stored outside of the application storage
     * or no installer is available on the device.
     */
    @Suppress("DEPRECATION")
    fun install(file: File): Boolean {
        if (!isInsideApplicationStorage(file)) {
            Log.e(TAG, "Refused to install an apk outside of the application storage")
            return false
        }
        
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open the system installer", e)
            false
        }
    }
    
    private fun isInsideApplicationStorage(file: File): Boolean = runCatching {
        val path = file.canonicalPath
        applicationDirectories().any { path.startsWith(it) }
    }.getOrDefault(false)
    
    private fun applicationDirectories(): List<String> = listOfNotNull(
        context.filesDir,
        context.cacheDir,
        context.getExternalFilesDir(null),
        context.externalCacheDir
    ).map { it.canonicalPath + File.separator }
    
    private companion object {
        const val TAG = "ApkInstaller"
    }
}
