/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.aiwazian.messenger.ApkInstallActivity
import com.aiwazian.messenger.ApkInstallPermissionActivity
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
    
    /** Whether the user allows this application to install other applications. */
    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()
    
    /**
     * Copies [file] into an installation session and commits it. The system asks
     * the user to confirm the installation of a committed session.
     *
     * Without the install permission [ApkInstallPermissionActivity] takes over: it
     * opens the system settings and repeats the installation once the permission
     * is granted.
     */
    suspend fun install(file: File): ApkInstallResult = withContext(Dispatchers.IO) {
        if (!isInsideApplicationStorage(file)) {
            Log.e(TAG, "Refused to install an apk outside of the application storage")
            return@withContext ApkInstallResult.UNTRUSTED_LOCATION
        }
        
        if (!canInstallPackages()) {
            requestInstallPermission(file)
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
    
    /**
     * Sends the status of a session to [ApkInstallActivity].
     *
     * The system installer shows the progress and the result of an installation
     * only when its confirmation is started from an activity, so the status is
     * delivered to an activity instead of a broadcast receiver.
     */
    private fun createStatusReceiver(sessionId: Int): IntentSender {
        val intent = Intent(context, ApkInstallActivity::class.java).apply {
            action = ApkInstallActivity.ACTION_INSTALL_STATUS
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getActivity(
            context, sessionId, intent, flags, activityStartOptions()
        ).intentSender
    }
    
    /**
     * Delegates the background activity launch privileges of this application to
     * the system installer.
     *
     * Since Android 15 the creator of a pending intent has to grant them
     * explicitly. Without it the system drops the start of [ApkInstallActivity]
     * with a "Background activity launch blocked" message and the installation
     * silently stops before its confirmation.
     */
    private fun activityStartOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return null
        }
        
        val options = ActivityOptions.makeBasic()
            .setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
        
        return options.toBundle()
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
    
    private fun requestInstallPermission(file: File) {
        val intent = Intent(context, ApkInstallPermissionActivity::class.java).apply {
            putExtra(ApkInstallPermissionActivity.EXTRA_APK_PATH, file.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot ask for the install permission", e)
        }
    }
    
    private companion object {
        const val TAG = "ApkInstaller"
        const val SESSION_ENTRY_NAME = "package"
        const val BUFFER_SIZE = 16 * 1024
    }
}
