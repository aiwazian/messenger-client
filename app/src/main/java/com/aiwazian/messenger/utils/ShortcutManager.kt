/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    
    private val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    
    fun createChatShortcut(
        chatId: Long,
        chatName: String
    ) {
        if (!shortcutManager.isRequestPinShortcutSupported) {
            return
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
            .setIntent(createChatIntent(chatId))
            .build()
        
        /*
         * Ярлык этого чата мог быть закреплён прежней версией приложения, и в нём
         * до сих пор лежит старый intent: система не перезаписывает его при
         * повторном requestPinShortcut. Без этого обновления такой ярлык остался бы
         * нерабочим навсегда.
         *
         * Для незакреплённого чата вызов ничего не делает.
         */
        shortcutManager.updateShortcuts(listOf(shortcut))
        
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
    
    /**
     * Intent, по которому ярлык открывает сам чат, а не главный экран.
     *
     * chatId уходит именно как Long: [MainActivity] читает extra через getLongExtra,
     * а на значении другого типа тот молча возвращает default, из-за чего переход в
     * чат терялся и открывался только главный экран.
     *
     * Флаги нужны для второго случая — приложение уже запущено. NEW_TASK без
     * CLEAR_TOP и SINGLE_TOP просто поднимает существующую задачу, onNewIntent при
     * этом не вызывается, и чат снова не открывается.
     */
    private fun createChatIntent(chatId: Long): Intent {
        return Intent(
            context,
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(
                MainActivity.EXTRA_CHAT_ID,
                chatId
            )
        }
    }
}
