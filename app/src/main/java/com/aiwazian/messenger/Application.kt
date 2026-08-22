/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.utils.FailedSendRetrier
import com.aiwazian.messenger.utils.PendingSendResumer
import com.aiwazian.messenger.utils.SessionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.yandex.mobile.ads.common.YandexAds
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var realtimeEventSyncService: RealtimeEventSyncService
    
    @Inject
    lateinit var pendingSendResumer: PendingSendResumer
    
    @Inject
    lateinit var failedSendRetrier: FailedSendRetrier
    
    override fun onCreate() {
        super.onCreate()
        
        SessionManager.init(authRepository)
        
        // Прошлый запуск могли убить посреди отправки файлов: сообщение и
        // вложения доедут сами, без ручного повтора.
        pendingSendResumer.resume()
        
        // Текст оборванной отправки лежит в самом сообщении, поэтому такие
        // сообщения дошлются при открытии своего чата.
        failedSendRetrier.start()
        
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        
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
