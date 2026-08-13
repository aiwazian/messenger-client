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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.utils.ActiveChatTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val MAX_MESSAGES = 5
        private const val ICON_SIZE_DP = 192
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
    
    private fun Bitmap.cropToCircle(): Bitmap {
        val sourceBitmap = if (config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false)
        } else {
            this
        }
        
        val size = minOf(sourceBitmap.width, sourceBitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = (size - sourceBitmap.width) / 2f
        val top = (size - sourceBitmap.height) / 2f
        canvas.drawBitmap(sourceBitmap, left, top, paint)
        
        if (sourceBitmap !== this) {
            sourceBitmap.recycle()
        }
        
        return output
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
        val shortcutId = "chat_$chatId"
        val person = createPerson(chatName, avatarBitmap, chatId)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("chatId", chatId)
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
    
    fun showMessageNotification(
        chatId: Long,
        title: String,
        body: String,
        avatarUri: Uri? = null
    ) {
        scope.launch {
            /*
             * Чат уже открыт на экране: пользователь читает эти сообщения прямо
             * сейчас. Проверка стоит здесь, а не только в обработчике вебсокета,
             * чтобы её не обходил пуш от Firebase, пришедший в момент, когда сокет
             * переподключался.
             */
            if (ActiveChatTracker.activeChatId.value == chatId) return@launch
            
            val chatInfo = when (ChatType.fromId(chatId)) {
                ChatType.PRIVATE -> database.userDao().getWithAvatars(chatId)?.let {
                    val name = "${it.user.firstName} ${it.user.lastName.orEmpty()}".trim()
                    val uri = it.avatars.firstOrNull()?.file?.path?.toUri()
                    name to uri
                }
                
                ChatType.GROUP -> database.groupDao().getWithAvatars(chatId)?.let {
                    val uri = it.avatars.firstOrNull()?.file?.path?.toUri()
                    it.group.name to uri
                }
                
                ChatType.CHANNEL -> database.channelDao().getWithAvatars(chatId)?.let {
                    val uri = it.avatars.firstOrNull()?.file?.path?.toUri()
                    it.channel.name to uri
                }
                
                else -> null
            }
            
            val resolvedTitle = chatInfo?.first ?: title
            val resolvedAvatarUri = chatInfo?.second ?: avatarUri
            
            val rawAvatar = loadAvatar(context, resolvedAvatarUri)
            val circularAvatar = rawAvatar?.cropToCircle()
            
            createOrUpdateChatShortcut(context, chatId, resolvedTitle, circularAvatar)
            
            val person = createPerson(resolvedTitle, circularAvatar, chatId)
            
            val history = loadMessages(context, chatId).toMutableList()
            val newMessage = MessageData(body, System.currentTimeMillis())
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
                putExtra("chatId", chatId)
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
                .setShortcutId("chat_$chatId")
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
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("chat_$chatId"))
    }
    
    private fun loadMessages(context: Context, chatId: Long): List<MessageData> {
        val prefs = context.getSharedPreferences("NOTIFICATIONS", Context.MODE_PRIVATE)
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
        context.getSharedPreferences("NOTIFICATIONS", Context.MODE_PRIVATE).edit {
            putString(chatId.toString(), jsonArray.toString())
        }
    }
    
    private fun getChannel(chatId: Long) = when (ChatType.fromId(chatId)) {
        ChatType.PRIVATE -> "private"
        ChatType.CHANNEL -> "channels"
        ChatType.GROUP -> "groups"
        ChatType.UNKNOWN -> "other"
    }
    
    private suspend fun loadAvatar(
        context: Context,
        uri: Uri?
    ): Bitmap? = withContext(Dispatchers.IO) {
        uri ?: return@withContext null
        
        var bitmap: Bitmap?
        
        val cached = context.imageLoader.memoryCache?.get(MemoryCache.Key(uri.toString()))?.bitmap
        bitmap = cached
        
        if (bitmap == null) {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(ICON_SIZE_DP)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                bitmap = result.drawable.toBitmap()
            }
        }
        
        bitmap?.let { bmp ->
            val softwareBitmap = if (bmp.config == Bitmap.Config.HARDWARE) {
                bmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bmp
            }
            
            val size = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
            softwareBitmap.scale(size, size)
        }
    }
}
