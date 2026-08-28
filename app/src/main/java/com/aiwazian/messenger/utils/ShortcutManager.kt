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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatAvatarIconLoader: ChatAvatarIconLoader
) {
    private val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    
    /*
     * Аватарку надо прочитать из Room и раскодировать в bitmap, поэтому ярлык собирается
     * в фоне. Scope свой, а не экранный: запрос на закрепление живёт дольше того экрана,
     * с которого его позвали.
     */
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun createChatShortcut(chatId: Long, chatName: String) {
        if (!shortcutManager.isRequestPinShortcutSupported) {
            return
        }
        
        scope.launch {
            val shortcut = buildShortcut(chatId, chatName, loadChatIcon(chatId))
            
            /* Починит intent у ярлыка, который уже закреплён со старой версией приложения. */
            shortcutManager.updateShortcuts(listOf(shortcut))
            
            val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successCallback = PendingIntent.getBroadcast(
                context, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(shortcut, successCallback.intentSender)
        }
    }
    
    /**
     * Подтянуть на закреплённый ярлык свежую аватарку и имя чата.
     *
     * Незакреплённые id [android.content.pm.ShortcutManager.updateShortcuts] игнорирует
     * молча, но декодировать картинку впустую незачем, поэтому сначала проверяем список
     * закреплённых. Пока новой аватарки нет, иконку не трогаем: старая лучше логотипа.
     */
    fun refreshPinnedChatShortcut(chatId: Long) {
        scope.launch {
            val shortcutId = chatId.toString()
            
            if (shortcutManager.pinnedShortcuts.none { it.id == shortcutId }) {
                return@launch
            }
            
            val chatAvatar = chatAvatarIconLoader.resolveChatAvatar(chatId)
            val chatName = chatAvatar.chatName?.takeIf { it.isNotBlank() } ?: return@launch
            val bitmap =
                chatAvatarIconLoader.loadCircleAvatar(chatAvatar.avatarUri) ?: return@launch
            
            shortcutManager.updateShortcuts(
                listOf(buildShortcut(chatId, chatName, Icon.createWithBitmap(bitmap)))
            )
        }
    }
    
    /**
     * Иконка ярлыка — круглая активная аватарка чата, как в уведомлениях.
     * Логотип приложения остаётся запасным вариантом: у чата может не быть аватарки.
     */
    private suspend fun loadChatIcon(chatId: Long): Icon {
        val avatarUri = chatAvatarIconLoader.resolveChatAvatar(chatId).avatarUri
        val bitmap = chatAvatarIconLoader.loadCircleAvatar(avatarUri)
        
        return if (bitmap != null) {
            Icon.createWithBitmap(bitmap)
        } else {
            Icon.createWithResource(context, R.mipmap.new_app_icon)
        }
    }
    
    private fun buildShortcut(
        chatId: Long,
        chatName: String,
        icon: Icon
    ): ShortcutInfo {
        return ShortcutInfo.Builder(context, chatId.toString())
            .setShortLabel(chatName)
            .setLongLabel(chatName)
            .setIcon(icon)
            .setIntent(createChatIntent(chatId))
            .build()
    }
    
    private fun createChatIntent(chatId: Long): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_CHAT_ID, chatId)
        }
    }
}
