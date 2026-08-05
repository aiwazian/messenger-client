/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat

/**
 * Shows the system screens of an installation session.
 *
 * The confirmation of a session is started from this activity, which keeps the
 * progress and the result of the installation on screen. A session that is
 * confirmed from a broadcast receiver closes the system installer right after
 * the confirmation.
 */
class ApkInstallActivity : ComponentActivity() {
    
    private val confirmationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStatus(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStatus(intent)
    }
    
    private fun handleStatus(intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) {
            finish()
            return
        }
        
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
        )
        
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> confirmInstallation(intent)
            
            PackageInstaller.STATUS_SUCCESS -> finish()
            
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(TAG, "Installation failed with status $status: $message")
                Toast.makeText(this, INSTALL_ERROR, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun confirmInstallation(intent: Intent) {
        val confirmation = IntentCompat.getParcelableExtra(
            intent, Intent.EXTRA_INTENT, Intent::class.java
        )
        
        if (confirmation == null) {
            Log.e(TAG, "The installation session has no confirmation intent")
            finish()
            return
        }
        
        try {
            confirmationLauncher.launch(confirmation)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot show the installation confirmation", e)
            finish()
        }
    }
    
    companion object {
        const val ACTION_INSTALL_STATUS = "com.aiwazian.messenger.APK_INSTALL_STATUS"
        
        private const val TAG = "ApkInstallActivity"
        private const val INSTALL_ERROR = "Cannot install this file"
    }
}
