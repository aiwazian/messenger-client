/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateManager(
    private val activity: ComponentActivity,
    private val onUpdateReadyToInstall: () -> Unit
) : DefaultLifecycleObserver {
    
    private val updateManager = AppUpdateManagerFactory.create(activity)
    
    private var updateFlowStarted = false

    private var checksEnabled = false
    
    private val updateLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.i(TAG, "Update flow closed by user: ${result.resultCode}")
        }
    }
    
    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> onUpdateReadyToInstall()
            InstallStatus.FAILED -> Log.e(
                TAG, "Update download failed: ${state.installErrorCode()}"
            )
            
            else -> Unit
        }
    }
    
    init {
        updateManager.registerListener(installListener)
        activity.lifecycle.addObserver(this)
    }
    
    override fun onResume(owner: LifecycleOwner) {
        if (checksEnabled) {
            checkForUpdate()
        }
    }

    fun enableUpdateChecks() {
        checksEnabled = true
        checkForUpdate()
    }

    private fun checkForUpdate() {
        updateManager.appUpdateInfo
            .addOnSuccessListener { info -> handleUpdateInfo(info) }
            .addOnFailureListener { error ->
                Log.i(TAG, "Update check is not available", error)
            }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        updateManager.unregisterListener(installListener)
    }
    
    fun completeUpdate() {
        updateManager.completeUpdate()
    }
    
    private fun handleUpdateInfo(info: AppUpdateInfo) {
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            onUpdateReadyToInstall()
            return
        }
        
        if (updateFlowStarted) return
        
        val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        if (!isUpdateAvailable || !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return
        
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || activity.isFinishing) {
            return
        }
        
        updateFlowStarted = true
        
        updateManager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }
    
    private companion object {
        const val TAG = "InAppUpdateManager"
    }
}
