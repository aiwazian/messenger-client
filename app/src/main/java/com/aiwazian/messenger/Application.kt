/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aiwazian.messenger.analytics.AnalyticsTracker
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.utils.SessionManager
import com.yandex.mobile.ads.common.YandexAds
import dagger.hilt.android.HiltAndroidApp
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var realtimeEventSyncService: RealtimeEventSyncService
    
    override fun onCreate() {
        super.onCreate()
        
        SessionManager.init(authRepository)
        AnalyticsTracker.init(this)
        if (!BuildConfig.DEBUG) {
            val config = AppMetricaConfig.newConfigBuilder("a68ca89d-5f0f-4637-9132-d49550bd5471")
                .withLocationTracking(true)
                .build()
            AppMetrica.activate(this, config)
            AppMetrica.enableActivityAutoTracking(this)
        }
        YandexAds.initialize(this) { }
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
