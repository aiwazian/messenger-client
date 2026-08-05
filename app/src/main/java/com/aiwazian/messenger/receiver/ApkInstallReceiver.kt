/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat

/**
 * Reports the status of the installation sessions created by the application.
 */
class ApkInstallReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) {
            return
        }
        
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
        )
        
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> confirmInstallation(context, intent)
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(TAG, "Installation failed with status $status: $message")
                Toast.makeText(context, "Cannot install this file", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun confirmInstallation(context: Context, intent: Intent) {
        val confirmation = IntentCompat.getParcelableExtra(
            intent, Intent.EXTRA_INTENT, Intent::class.java
        ) ?: return
        
        confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(confirmation)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot show the installation confirmation", e)
        }
    }
    
    companion object {
        const val ACTION_INSTALL_STATUS = "com.aiwazian.messenger.APK_INSTALL_STATUS"
        private const val TAG = "ApkInstallReceiver"
    }
}
