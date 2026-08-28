/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.entity.AvatarWithFile
import com.aiwazian.messenger.enums.ChatType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Достаёт активную аватарку чата и готовит из неё круглую иконку.
 *
 * Тип чата определяется по первой цифре id через [ChatType.fromId], поэтому один и тот
 * же вызов работает для личных чатов, групп и каналов.
 */
@Singleton
class ChatAvatarIconLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    
    /** Имя чата и путь к его активной аватарке. */
    data class ChatAvatar(
        val chatName: String?,
        val avatarUri: Uri?
    )
    
    suspend fun resolveChatAvatar(chatId: Long): ChatAvatar = withContext(Dispatchers.IO) {
        val resolved = when (ChatType.fromId(chatId)) {
            ChatType.PRIVATE -> database.userDao().getWithAvatars(chatId)?.let { userWithAvatars ->
                val user = userWithAvatars.user
                
                ChatAvatar(
                    chatName = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                    avatarUri = userWithAvatars.avatars.activeAvatarUri()
                )
            }
            
            ChatType.GROUP -> database.groupDao().getWithAvatars(chatId)?.let { groupWithAvatars ->
                ChatAvatar(
                    chatName = groupWithAvatars.group.name,
                    avatarUri = groupWithAvatars.avatars.activeAvatarUri()
                )
            }
            
            ChatType.CHANNEL -> database.channelDao().getWithAvatars(chatId)
                ?.let { channelWithAvatars ->
                    ChatAvatar(
                        chatName = channelWithAvatars.channel.name,
                        avatarUri = channelWithAvatars.avatars.activeAvatarUri()
                    )
                }
            
            ChatType.UNKNOWN -> null
        }
        
        resolved ?: ChatAvatar(chatName = null, avatarUri = null)
    }
    
    /**
     * Загрузить аватарку и обрезать её в круг.
     *
     * Возвращает null, если аватарки нет или файл ещё не скачался: чем заменить иконку,
     * решает вызывающий.
     */
    suspend fun loadCircleAvatar(uri: Uri?): Bitmap? {
        val bitmap = loadAvatar(uri) ?: return null
        
        return bitmap.cropToCircle()
    }
    
    /**
     * Активная аватарка — это фотография с самым большим sortOrder.
     *
     * Сервер нумерует фотографии по порядку добавления и новой ставит sortOrder
     * предыдущей + 1, а список отдаёт по возрастанию. Значит текущая аватарка профиля
     * лежит в конце списка, а не в начале.
     *
     * Если её файл ещё не скачан, берём ближайшую скачанную — пустая иконка хуже
     * слегка устаревшей.
     */
    private fun List<AvatarWithFile>.activeAvatarUri(): Uri? = this
        .sortedByDescending { avatarWithFile -> avatarWithFile.avatar.sortOrder }
        .firstNotNullOfOrNull { avatarWithFile ->
            avatarWithFile.file?.path?.takeIf { path -> path.isNotBlank() }
        }
        ?.toUri()
    
    private suspend fun loadAvatar(uri: Uri?): Bitmap? = withContext(Dispatchers.IO) {
        uri ?: return@withContext null
        
        try {
            val size = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
            
            /*
             * Экран профиля только что показал эту аватарку, поэтому обычно она уже
             * лежит в памяти Coil и второй раз с диска не читается.
             */
            val cached =
                context.imageLoader.memoryCache?.get(MemoryCache.Key(uri.toString()))?.bitmap
            
            val loaded = cached ?: run {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(ICON_SIZE_DP)
                    .build()
                
                (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
            } ?: return@withContext null
            
            /* HARDWARE-битмап нельзя рисовать на Canvas, поэтому копируем его в память. */
            val softwareBitmap = if (loaded.config == Bitmap.Config.HARDWARE) {
                loaded.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                loaded
            } ?: return@withContext null
            
            softwareBitmap.scale(size, size)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить аватарку $uri", e)
            null
        }
    }
    
    private fun Bitmap.cropToCircle(): Bitmap {
        val source = if (config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false) ?: return this
        } else {
            this
        }
        
        val size = minOf(source.width, source.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f
        
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(
            source,
            (size - source.width) / 2f,
            (size - source.height) / 2f,
            paint
        )
        
        if (source !== this) {
            source.recycle()
        }
        
        return output
    }
    
    private companion object {
        const val ICON_SIZE_DP = 192
        const val TAG = "ChatAvatarIconLoader"
    }
}
