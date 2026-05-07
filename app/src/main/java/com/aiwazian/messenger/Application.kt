/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger

import android.app.Application
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.socket.RealtimeEventSyncService
import com.aiwazian.messenger.utils.SessionManager
import com.google.firebase.FirebaseApp
import com.yandex.mobile.ads.common.YandexAds
import dagger.hilt.android.HiltAndroidApp
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
    }
}
