/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.receiver.ApkInstallReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ApkInstallResult {
    STARTED,
    PERMISSION_REQUIRED,
    UNTRUSTED_LOCATION,
    FAILED
}

/**
 * Installs apk files through the system [PackageInstaller].
 *
 * Only files inside the application storage are accepted: a file in shared
 * storage can be replaced by another application between the moment it is
 * checked and the moment it is installed.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    /**
     * Copies [file] into an installation session and commits it. The system asks
     * the user to confirm the installation of a committed session.
     */
    suspend fun install(file: File): ApkInstallResult = withContext(Dispatchers.IO) {
        if (!isInsideApplicationStorage(file)) {
            Log.e(TAG, "Refused to install an apk outside of the application storage")
            return@withContext ApkInstallResult.UNTRUSTED_LOCATION
        }
        
        if (!context.packageManager.canRequestPackageInstalls()) {
            requestInstallPermission()
            return@withContext ApkInstallResult.PERMISSION_REQUIRED
        }
        
        try {
            commitSession(file)
            ApkInstallResult.STARTED
        } catch (e: Exception) {
            Log.e(TAG, "Error installing an apk", e)
            ApkInstallResult.FAILED
        }
    }
    
    private fun commitSession(file: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        
        packageInstaller.openSession(sessionId).use { session ->
            session.openWrite(SESSION_ENTRY_NAME, 0, file.length()).use { output ->
                file.inputStream().use { input -> input.copyTo(output, BUFFER_SIZE) }
                session.fsync(output)
            }
            
            session.commit(createStatusReceiver(sessionId))
        }
    }
    
    private fun createStatusReceiver(sessionId: Int): IntentSender {
        val intent = Intent(context, ApkInstallReceiver::class.java).apply {
            action = ApkInstallReceiver.ACTION_INSTALL_STATUS
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(context, sessionId, intent, flags).intentSender
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
    
    private fun requestInstallPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open the unknown app sources settings", e)
        }
    }
    
    private companion object {
        const val TAG = "ApkInstaller"
        const val SESSION_ENTRY_NAME = "package"
        const val BUFFER_SIZE = 16 * 1024
    }
}
