/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType

object NotificationHelper {
    
    fun showMessageNotification(
        context: Context,
        chatId: Long,
        title: String,
        body: String
    ) {
        val messages = loadMessages(context, chatId)
            .apply { add(body) }
            .takeLast(5)
            .toMutableList()
        saveMessages(context, chatId, messages)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        messages.forEach { style.addLine(it) }
        
        val notification = NotificationCompat.Builder(context, getChannel(chatId))
            .setSmallIcon(R.mipmap.new_app_icon_round)
            .setContentTitle(title)
            .setContentText(messages.last())
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setNumber(messages.size)
            .build()
        
        val notificationId = chatId.toInt()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
    
    fun clearChatNotifications(context: Context, chatId: Long) {
        saveMessages(context, chatId, mutableListOf())
        NotificationManagerCompat.from(context).cancel(chatId.toInt())
    }
    
    private fun loadMessages(context: Context, chatId: Long): MutableList<String> {
        val prefs = context.getSharedPreferences("NOTIFICATIONS", Context.MODE_PRIVATE)
        val raw = prefs.getString(chatId.toString(), null) ?: return mutableListOf()
        return raw.split("\u0000").toMutableList()
    }
    
    private fun saveMessages(context: Context, chatId: Long, messages: List<String>) {
        context.getSharedPreferences("NOTIFICATIONS", Context.MODE_PRIVATE).edit {
            putString(chatId.toString(), messages.joinToString("\u0000"))
        }
    }
    
    private fun getChannel(chatId: Long) = when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> "private"
        ChatType.CHANNEL -> "channels"
        ChatType.GROUP -> "groups"
        ChatType.UNKNOWN -> "other"
    }
}
