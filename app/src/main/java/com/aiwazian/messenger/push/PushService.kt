/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var pushRegistrar: PushRegistrar
    
    /**
     * Замена onNewToken: вместо registration token теперь приходит Firebase
     * Installation ID, и именно по нему адресуется уведомление.
     *
     * Колбек вызывается после register(), при смене FID (переустановка, очистка данных,
     * восстановление на другом устройстве) и при плановой синхронизации SDK.
     * Сохранением занимается PushRegistrar: сервис к ответу сервера может быть уже убит.
     */
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        
        Log.d("PushService", "New installation id")
        pushRegistrar.onRegistered(installationId)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"] ?: return
        val chatId = data["chatId"]?.toLongOrNull() ?: return
        
        notificationHelper.showMessageNotification(chatId, title, body)
    }
}
