/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import javax.inject.Inject

@AndroidEntryPoint
class PushService : RuStoreMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PushService", "New token $token")
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("PushService", message.notification.toString())
        
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"] ?: return
        val chatId = data["chatId"]?.toLong() ?: return
        
        notificationHelper.showMessageNotification(chatId, title, body)
    }
}
