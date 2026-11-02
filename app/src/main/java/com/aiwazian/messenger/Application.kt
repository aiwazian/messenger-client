/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Looper
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.network.dns.DnsApplier
import com.aiwazian.messenger.network.dns.DnsController
import com.aiwazian.messenger.network.proxy.ProxyApplier
import com.aiwazian.messenger.network.proxy.applyAppProxy
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
import okhttp3.OkHttpClient

@HiltAndroidApp
class Application : Application(), SingletonImageLoader.Factory {

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
    lateinit var dnsApplier: Lazy<DnsApplier>

    @Inject
    lateinit var proxyApplier: Lazy<ProxyApplier>

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        if (android.app.Application.getProcessName().contains(PdfSandboxProcessMarker)) return

        SessionManager.init(authRepository, restoreCredentialManager)

        appScope.launch {
            dnsApplier.get().start()
        }

        appScope.launch {
            proxyApplier.get().start()
        }

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
    
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .dns(DnsController)
                                .applyAppProxy()
                                .build()
                        }
                    )
                )
            }
            .build()
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

    private companion object {
        const val PdfSandboxProcessMarker = "androidx.pdf"
    }
}
