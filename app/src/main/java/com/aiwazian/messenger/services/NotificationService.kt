package com.aiwazian.messenger.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.Notification
import com.aiwazian.messenger.data.NotificationTokenRequest
import com.aiwazian.messenger.interfaces.NotificationService
import com.aiwazian.messenger.utils.ChatState
import com.aiwazian.messenger.utils.NotificationChannelConstants
import com.aiwazian.messenger.utils.VibrationPattern
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private data class ChatNotificationData(
    val chatId: Long,
    val title: String,
    val messages: MutableList<String>
)

private val chatNotifications = mutableMapOf<Long, ChatNotificationData>()

class NotificationService : FirebaseMessagingService(), NotificationService {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val personalMessages = NotificationChannelConstants.PERSONAL_MESSAGES

        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(
            personalMessages.id,
            personalMessages.name,
            importance
        ).apply {
            description = personalMessages.description
            enableVibration(true)
            vibrationPattern = VibrationPattern.Notification
        }

        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        CoroutineScope(Dispatchers.IO).launch {
            sendTokenToServer(token)
        }
    }
    
    suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (token != null) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(IllegalStateException("Token is null"))
                    }
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Unknown error")
                    )
                }
            }
        }
    }
    
    suspend fun sendTokenToServer(token: String) {
        try {
            val request = NotificationTokenRequest(token)
            val response = RetrofitInstance.api.updateFcmToken(request)
            
            if (response.isSuccessful) {
                Log.d(
                    "NotificationService",
                    "Токен обновлен"
                )
            } else {
                Log.e(
                    "NotificationService",
                    "Ошибка при обновлении токена: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.e(
                "NotificationService",
                "Ошибка при получении токена",
                e
            )
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val chatId = remoteMessage.data["chatId"]?.toLongOrNull() ?: return
        val title = remoteMessage.data["title"] ?: "Messenger"
        val body = remoteMessage.data["body"] ?: ""
        
        if (!ChatState.isChatOpen(chatId)) {
            val chatData = chatNotifications.getOrPut(chatId) {
                ChatNotificationData(
                    chatId = chatId,
                    title = title,
                    messages = mutableListOf()
                )
            }
            
            if (chatData.messages.size >= 5) {
                chatData.messages.removeAt(0)
            }
            
            chatData.messages.add(body)
            
            val notification = Notification(
                chatId,
                title,
                body
            )
            
            showNotification(
                notification = notification,
                messages = chatData.messages
            )
        }
    }
    
    override fun showNotification(
        notification: Notification,
        messages: List<String>
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = NotificationChannelConstants.PERSONAL_MESSAGES.id
        val groupKey = "CHAT_GROUP_${notification.chatId}"
        
        val notificationText = messages.joinToString(separator = "\n")
        
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(notificationText)
            .setSummaryText(notification.title)
        
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            putExtra(
                "chatId",
                notification.chatId
            )
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            notification.chatId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val summaryNotification = NotificationCompat.Builder(
            this,
            channelId
        )
            .setContentTitle(notification.title)
            .setContentText(notificationText)
            .setStyle(bigTextStyle)
            .setSmallIcon(R.mipmap.new_app_icon_round)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentIntent(pendingIntent)
            .setVibrate(VibrationPattern.Notification)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(
            notification.chatId.toInt(),
            summaryNotification
        )
    }
}