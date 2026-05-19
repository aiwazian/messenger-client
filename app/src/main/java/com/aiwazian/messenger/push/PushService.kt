/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import javax.inject.Inject

@AndroidEntryPoint
class PushService : RuStoreMessagingService() {
    
    @Inject
    lateinit var accountDao: AccountDao
    
    @Inject
    lateinit var sessionRepository: SessionRepository
    
    lateinit var notificationManager: NotificationManager
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(NotificationManager::class.java)
        
        scope.launch {
            accountDao.getCurrentAccount()?.let { account ->
                RuStorePushClient.getToken().addOnSuccessListener { token ->
                    if (account.fcmToken.isNullOrBlank()) {
                        scope.launch {
                            sessionRepository.updateFcmToken(token).onSuccess {
                                accountDao.update(account.copy(fcmToken = token))
                            }.onFailure {
                                Log.e("PushService", "Error saving token", it)
                            }
                        }
                    }
                    Log.d("PushService", token)
                }.addOnFailureListener { th ->
                    Log.e("PushService", "Error getting token", th)
                }
            }
        }
    }
    
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
        
        val channelId = getChannel(chatId)
        val groupKey = "CHAT_GROUP_${chatId}"
        
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(body).setSummaryText(body)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("chatId", chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val summaryNotification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.mipmap.new_app_icon_round)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setVibrate(VibrationPattern.Notification)
                .setAutoCancel(true)
                .build()
        
        notificationManager.notify(1, summaryNotification)
    }
    
    private fun getChannel(chatId: Long) = when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> "private"
        ChatType.CHANNEL -> "channels"
        ChatType.GROUP -> "groups"
        ChatType.UNKNOWN -> "other"
    }
}
