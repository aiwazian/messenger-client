/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.utils.VibrationPattern

object NotificationHelper {
    
    fun showMessageNotification(
        context: Context,
        chatId: Long,
        title: String,
        body: String
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        val channelId = getChannel(chatId)
        val groupKey = "CHAT_GROUP_${chatId}"
        
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(body).setSummaryText(body)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("chatId", chatId.toString())
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val summaryNotification =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.mipmap.new_app_icon_round)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(VibrationPattern.Notification)
                .setAutoCancel(true)
                .build()
        
        notificationManager.notify(chatId.toInt(), summaryNotification)
    }
    
    private fun getChannel(chatId: Long) = when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> "private"
        ChatType.CHANNEL -> "channels"
        ChatType.GROUP -> "groups"
        ChatType.UNKNOWN -> "other"
    }
}
