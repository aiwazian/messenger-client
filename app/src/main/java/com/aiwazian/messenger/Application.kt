/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Looper
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.utils.FailedSendRetrier
import com.aiwazian.messenger.utils.PendingSendResumer
import com.aiwazian.messenger.utils.RestoreCredentialManager
import com.aiwazian.messenger.utils.SessionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.yandex.mobile.ads.common.YandexAds
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class Application : Application() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var restoreCredentialManager: RestoreCredentialManager

    @Inject
    lateinit var realtimeEventSyncService: Lazy<RealtimeEventSyncService>

    @Inject
    lateinit var pendingSendResumer: Lazy<PendingSendResumer>

    @Inject
    lateinit var failedSendRetrier: Lazy<FailedSendRetrier>

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope
    
    override fun onCreate() {
        super.onCreate()

        SessionManager.init(authRepository, restoreCredentialManager)

        appScope.launch {
            realtimeEventSyncService.get()
        }

        appScope.launch {
            pendingSendResumer.get().resume()
        }

        appScope.launch {
            failedSendRetrier.get().start()
        }

        appScope.launch {
            FirebaseAnalytics.getInstance(this@Application).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        Looper.getMainLooper().queue.addIdleHandler {
            YandexAds.initialize(this@Application) { }
            false
        }

        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        
        val messagesChannel = NotificationChannel(
            "private",
            "Private Chats",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        val groupsChannel = NotificationChannel(
            "groups",
            "Groups",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        val systemChannel = NotificationChannel(
            "channels",
            "Channels",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        manager.createNotificationChannel(messagesChannel)
        manager.createNotificationChannel(groupsChannel)
        manager.createNotificationChannel(systemChannel)
    }
}
