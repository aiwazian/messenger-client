/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var pushRegistrar: PushRegistrar
    
    @Inject
    lateinit var accountDao: AccountDao
    
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
        
        if (!isForCurrentAccount(data["userId"]?.toLongOrNull())) {
            Log.d("PushService", "Notification for another account, skipped")
            return
        }
        
        val sendTime = data["sendTime"]?.toLongOrNull() ?: System.currentTimeMillis()
        
        notificationHelper.showMessageNotification(
            chatId = chatId,
            title = title,
            body = body,
            sendTime = sendTime
        )
    }
    
    private fun isForCurrentAccount(recipientId: Long?): Boolean {
        recipientId ?: return true
        
        val currentUserId = runBlocking { accountDao.getCurrentAccount()?.userId }
        return currentUserId == recipientId
    }
}
