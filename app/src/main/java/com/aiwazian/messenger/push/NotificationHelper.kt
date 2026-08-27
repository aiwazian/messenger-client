/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.utils.ActiveChatTracker
import com.aiwazian.messenger.utils.ChatAvatarIconLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatAvatarIconLoader: ChatAvatarIconLoader,
    private val notificationSettingsRepository: NotificationSettingsRepository
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val MAX_MESSAGES = 5
        
        /** История показанных уведомлений по чатам: ключ — chatId. */
        private const val NOTIFICATIONS_PREFS = "NOTIFICATIONS"
        
        private const val CHAT_SHORTCUT_PREFIX = "chat_"
    }
    
    private data class MessageData(
        val text: String,
        val timestamp: Long
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("text", text)
            put("timestamp", timestamp)
        }
        
        companion object {
            fun fromJson(json: JSONObject) = MessageData(
                json.optString("text", ""),
                json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
    
    private fun createPerson(
        name: String,
        avatarBitmap: Bitmap?,
        chatId: Long
    ): Person {
        return Person.Builder()
            .setName(name)
            .setIcon(avatarBitmap?.let { IconCompat.createWithBitmap(it) })
            .setUri("user://$chatId")
            .build()
    }
    
    private fun createOrUpdateChatShortcut(
        context: Context,
        chatId: Long,
        chatName: String,
        avatarBitmap: Bitmap?
    ) {
        val shortcutId = "$CHAT_SHORTCUT_PREFIX$chatId"
        val person = createPerson(chatName, avatarBitmap, chatId)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(chatName.take(10))
            .setLongLabel(chatName)
            .setIntent(intent)
            .setPerson(person)
            .setIcon(avatarBitmap?.let { IconCompat.createWithBitmap(it) })
            .setLongLived(true)
            .build()
        
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }
    
    /**
     * @param sendTime когда сообщение было отправлено, а не когда пуш доехал. Уходит в
     * setWhen и в MessagingStyle, чтобы часовой давности сообщение не выглядело свежим.
     */
    fun showMessageNotification(
        chatId: Long,
        title: String,
        body: String,
        avatarUri: Uri? = null,
        sendTime: Long = System.currentTimeMillis()
    ) {
        scope.launch {
            /*
             * Чат уже открыт на экране: пользователь читает эти сообщения прямо
             * сейчас. Проверка стоит здесь, а не только в обработчике вебсокета,
             * чтобы её не обходил пуш от Firebase, пришедший в момент, когда сокет
             * переподключался.
             */
            if (ActiveChatTracker.activeChatId.value == chatId) return@launch
            
            /*
             * Вторая линия обороны после серверного фильтра. Настройка могла не
             * доехать до сервера, пуш мог уйти раньше её изменения, да и сама
             * отправка не мгновенная — а пользователь ждёт тишины сразу после
             * переключения тумблера.
             */
            if (!notificationSettingsRepository.isEnabledFor(chatId)) return@launch
            
            val chatAvatar = chatAvatarIconLoader.resolveChatAvatar(chatId)
            
            val resolvedTitle = chatAvatar.chatName?.takeIf { it.isNotBlank() } ?: title
            
            /* Чата ещё нет в базе — тогда в дело идёт аватарка из пуша. */
            val resolvedAvatarUri = chatAvatar.avatarUri ?: avatarUri
            
            val circularAvatar = chatAvatarIconLoader.loadCircleAvatar(resolvedAvatarUri)
            
            createOrUpdateChatShortcut(context, chatId, resolvedTitle, circularAvatar)
            
            val person = createPerson(resolvedTitle, circularAvatar, chatId)
            
            val history = loadMessages(context, chatId).toMutableList()
            val newMessage = MessageData(body, sendTime)
            history.add(newMessage)
            val trimmedHistory = history.takeLast(MAX_MESSAGES)
            saveMessages(context, chatId, trimmedHistory)
            
            val style = NotificationCompat.MessagingStyle(person)
                .setConversationTitle(resolvedTitle)
                .setGroupConversation(ChatType.fromId(chatId) != ChatType.PRIVATE)
            
            trimmedHistory.forEach { msg ->
                style.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        msg.text,
                        msg.timestamp,
                        person
                    )
                )
            }
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_CHAT_ID, chatId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                chatId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, getChannel(chatId))
                .setSmallIcon(R.mipmap.new_app_icon_round)
                .setStyle(style)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShortcutId("$CHAT_SHORTCUT_PREFIX$chatId")
                .setWhen(sendTime)
                .setShowWhen(true)
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
    }
    
    fun clearChatNotifications(chatId: Long) {
        saveMessages(context, chatId, emptyList())
        NotificationManagerCompat.from(context).cancel(chatId.toInt())
        ShortcutManagerCompat.removeDynamicShortcuts(
            context,
            listOf("$CHAT_SHORTCUT_PREFIX$chatId")
        )
    }
    
    /**
     * Убрать все уведомления и ярлыки чатов — при смене аккаунта или выходе.
     *
     * Шторка и ярлыки живут вне процесса и ничего не знают про аккаунты: без
     * этой чистки новый аккаунт видел бы чужие сообщения, а нажатие вело бы в чат,
     * которого у него нет.
     *
     * Удаляются только ярлыки чатов, а не все динамические: остальные к аккаунту не
     * привязаны, и терять их незачем.
     */
    fun clearAllNotifications() {
        val prefs = context.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
        val chatShortcutIds = prefs.all.keys.map { "$CHAT_SHORTCUT_PREFIX$it" }
        
        prefs.edit { clear() }
        
        NotificationManagerCompat.from(context).cancelAll()
        
        if (chatShortcutIds.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, chatShortcutIds)
        }
    }
    
    private fun loadMessages(context: Context, chatId: Long): List<MessageData> {
        val prefs = context.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(chatId.toString(), null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<MessageData>()
            for (i in 0 until jsonArray.length()) {
                list.add(MessageData.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    private fun saveMessages(context: Context, chatId: Long, messages: List<MessageData>) {
        val jsonArray = JSONArray()
        messages.forEach { jsonArray.put(it.toJson()) }
        context.getSharedPreferences(NOTIFICATIONS_PREFS, Context.MODE_PRIVATE).edit {
            putString(chatId.toString(), jsonArray.toString())
        }
    }
    
    private fun getChannel(chatId: Long) = when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> "private"
        ChatType.CHANNEL -> "channels"
        ChatType.GROUP -> "groups"
        ChatType.UNKNOWN -> "other"
    }
}
