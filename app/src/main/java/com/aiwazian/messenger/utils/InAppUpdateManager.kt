/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Необязательное обновление через Google Play (flexible in-app update).
 *
 * Взят именно FLEXIBLE, а не IMMEDIATE: Play показывает своё окно с предложением
 * обновиться, его можно закрыть и продолжить пользоваться текущей версией, а сама
 * загрузка идёт фоном и ничего не блокирует. IMMEDIATE закрыл бы приложение
 * экраном обновления и не дал бы его пропустить.
 *
 * Установка скачанного пакета требует перезапуска, поэтому класс не делает этого
 * сам: он только сообщает через [onUpdateReadyToInstall], а решение остаётся за
 * пользователем.
 */
class InAppUpdateManager(
    activity: ComponentActivity,
    private val onUpdateReadyToInstall: () -> Unit
) : DefaultLifecycleObserver {
    
    private val updateManager = AppUpdateManagerFactory.create(activity)
    
    /*
     * Окно Play предлагаем один раз за запуск: onResume срабатывает при каждом
     * возврате в приложение, и без флага человек ловил бы диалог снова и снова
     * сразу после того, как его закрыл.
     */
    private var updateFlowStarted = false
    
    private val updateLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        /*
         * Отказ — штатный сценарий, а не ошибка: обновление необязательное,
         * приложение продолжает работать на текущей версии.
         */
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
        updateManager.appUpdateInfo
            .addOnSuccessListener { info -> handleUpdateInfo(info) }
            .addOnFailureListener { error ->
                /*
                 * Вне Google Play (debug-сборка, установка из apk) сервис отвечает
                 * ошибкой. Это норма и не повод ругаться — просто нет обновлений.
                 */
                Log.i(TAG, "Update check is not available", error)
            }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        updateManager.unregisterListener(installListener)
    }
    
    /** Перезапуск и установка скачанного пакета — только по кнопке пользователя. */
    fun completeUpdate() {
        updateManager.completeUpdate()
    }
    
    private fun handleUpdateInfo(info: AppUpdateInfo) {
        /*
         * Загрузка могла завершиться, пока приложение было свёрнуто и никто не
         * видел предложение перезапуститься, поэтому состояние перепроверяем при
         * каждом возврате на экран.
         */
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            onUpdateReadyToInstall()
            return
        }
        
        if (updateFlowStarted) return
        
        val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        if (!isUpdateAvailable || !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return
        
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
