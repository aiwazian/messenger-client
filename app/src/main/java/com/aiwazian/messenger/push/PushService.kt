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
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PushService", "New token $token")
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
