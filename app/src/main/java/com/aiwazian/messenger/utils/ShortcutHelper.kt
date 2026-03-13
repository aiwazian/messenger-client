package com.aiwazian.messenger.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R

object ShortcutHelper {
    
    fun createChatShortcut(
        context: Context,
        chatId: Long,
        chatName: String
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        
        if (!shortcutManager.isRequestPinShortcutSupported) {
            return
        }
        
        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_VIEW
            putExtra(
                "chatId",
                chatId.toString()
            )
        }
        
        val shortcut = ShortcutInfo.Builder(
            context,
            chatId.toString()
        )
            .setShortLabel(chatName)
            .setLongLabel(chatName)
            .setIcon(
                Icon.createWithResource(
                    context,
                    R.mipmap.new_app_icon
                )
            )
            .setIntent(intent)
            .build()
        
        val pinnedShortcutCallbackIntent =
            shortcutManager.createShortcutResultIntent(shortcut)
        
        val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            pinnedShortcutCallbackIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        shortcutManager.requestPinShortcut(
            shortcut,
            successCallback.intentSender
        )
    }
}