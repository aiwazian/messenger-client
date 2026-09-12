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
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.entity.AvatarWithFile
import com.aiwazian.messenger.enums.ChatType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatAvatarIconLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    
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
    
    suspend fun loadCircleAvatar(uri: Uri?): Bitmap? {
        val bitmap = loadAvatar(uri) ?: return null
        
        return bitmap.cropToCircle()
    }
    
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
            
            val imageLoader = SingletonImageLoader.get(context)
            
            val cached = imageLoader.memoryCache
                ?.get(MemoryCache.Key(uri.toString()))
                ?.image
                ?.toBitmap()
            
            val loaded = cached ?: run {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(ICON_SIZE_DP)
                    .build()
                
                (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
            } ?: return@withContext null
            
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
