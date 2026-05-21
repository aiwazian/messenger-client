/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.utils.SessionManager
import com.google.firebase.FirebaseApp
import com.yandex.mobile.ads.common.YandexAds
import dagger.hilt.android.HiltAndroidApp
import ru.rustore.sdk.pushclient.RuStorePushClient
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var realtimeEventSyncService: RealtimeEventSyncService
    
    override fun onCreate() {
        super.onCreate()
        
        if (!::authRepository.isInitialized) {
            throw IllegalStateException("AuthRepository not initialized by Hilt")
        }
        
        SessionManager.init(authRepository)
        FirebaseApp.initializeApp(this)
        YandexAds.initialize(this) { }
        RuStorePushClient.init(
            application = this,
            projectId = "uW0av0Fk9guM2gIW8IwhhVwMxjReEMrt"
        )
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
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        val groupsChannel = NotificationChannel(
            "groups",
            "Groups",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        val systemChannel = NotificationChannel(
            "channels",
            "Channels",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        
        manager.createNotificationChannel(messagesChannel)
        manager.createNotificationChannel(groupsChannel)
        manager.createNotificationChannel(systemChannel)
    }
}
