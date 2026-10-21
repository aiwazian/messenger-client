/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.NotificationSettingsDao
import com.aiwazian.messenger.database.entity.NotificationSettingsEntity
import com.aiwazian.messenger.domain.ChatNotificationException
import com.aiwazian.messenger.domain.NotificationSettings
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.network.api.NotificationSettingsApi
import com.aiwazian.messenger.network.dto.UpdateChatNotificationSettingRequestDto
import com.aiwazian.messenger.network.dto.UpdateNotificationSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val notificationSettingsApi: NotificationSettingsApi,
    private val notificationSettingsDao: NotificationSettingsDao,
    private val chatDao: ChatDao,
    private val accountDao: AccountDao
) {
    
    private val chatExceptions = MutableStateFlow<List<ChatNotificationException>>(emptyList())

    fun observe(): Flow<NotificationSettings> = flow {
        val userId = currentUserId()
        
        if (userId == null) {
            emit(NotificationSettings())
            return@flow
        }
        
        emitAll(
            notificationSettingsDao.observe(userId)
                .map { it?.toDomain() ?: NotificationSettings() })
    }
    
    suspend fun refresh() {
        val userId = currentUserId() ?: return
        
        try {
            val response = notificationSettingsApi.getNotificationSettings()
            val settings = response.body()
            
            if (response.isSuccessful && settings != null) {
                notificationSettingsDao.upsert(settings.toDomain().toEntity(userId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить настройки уведомлений", e)
        }
    }

    suspend fun update(settings: NotificationSettings): Result<Unit> {
        val userId = currentUserId()
            ?: return Result.failure(IllegalStateException("Нет активного аккаунта"))
        
        val previous = notificationSettingsDao.get(userId)?.toDomain() ?: NotificationSettings()
        
        notificationSettingsDao.upsert(settings.toEntity(userId))
        
        return try {
            val response = notificationSettingsApi.updateNotificationSettings(
                UpdateNotificationSettingsRequestDto(
                    privateChats = settings.privateChats,
                    groups = settings.groups,
                    channels = settings.channels
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    notificationSettingsDao.upsert(it.toDomain().toEntity(userId))
                }
                
                Result.success(Unit)
            } else {
                notificationSettingsDao.upsert(previous.toEntity(userId))
                Result.failure(Exception("Failed to update notification settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            notificationSettingsDao.upsert(previous.toEntity(userId))
            Result.failure(e)
        }
    }

    suspend fun resetAll(): Result<Unit> {
        val userId = currentUserId()
            ?: return Result.failure(IllegalStateException("Нет активного аккаунта"))
        
        return try {
            val response = notificationSettingsApi.deleteAllChatNotificationSettings(category = null)
            
            if (!response.isSuccessful) {
                return Result.failure(
                    Exception("Failed to delete chat notification settings: ${response.code()}")
                )
            }
            
            val removed = chatExceptions.value
            chatExceptions.value = emptyList()
            removed.forEach { resetChatMutedToCategory(it.chatId) }
            
            val result = update(NotificationSettings())
        
            if (result.isSuccess) {
                chatDao.clearMuted(userId)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChatMuted(chatId: Long): Flow<Boolean> = flow {
        val userId = currentUserId()
        
        if (userId == null) {
            emit(false)
            return@flow
        }
        
        emitAll(chatDao.getChatByIdFlow(userId, chatId).map { it?.isMuted == true })
    }
    
    fun observeChatExceptions(): Flow<List<ChatNotificationException>> = chatExceptions.asStateFlow()
    
    suspend fun refreshChatExceptions() {
        try {
            val response = notificationSettingsApi.getChatNotificationSettings()
            
            if (response.isSuccessful) {
                chatExceptions.value = response.body().orEmpty()
                    .map { ChatNotificationException(it.chatId, it.enabled) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить исключения чатов", e)
        }
    }

    suspend fun setChatNotifications(chatId: Long, enabled: Boolean): Result<Unit> {
        val userId = currentUserId()
            ?: return Result.failure(IllegalStateException("Нет активного аккаунта"))
        
        val previousMuted = chatDao.isMuted(userId, chatId) ?: false
        val previousException = chatExceptions.value.firstOrNull { it.chatId == chatId }
        
        chatDao.setMuted(userId, chatId, !enabled)
        upsertException(chatId, enabled)
        
        return try {
            val response = notificationSettingsApi.setChatNotificationSetting(
                chatId, UpdateChatNotificationSettingRequestDto(enabled = enabled)
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                rollbackChatSetting(userId, chatId, previousMuted, previousException)
                Result.failure(Exception("Failed to update chat notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            rollbackChatSetting(userId, chatId, previousMuted, previousException)
            Result.failure(e)
        }
    }

    suspend fun removeChatException(chatId: Long): Result<Unit> {
        return try {
            val response = notificationSettingsApi.deleteChatNotificationSetting(chatId)
            
            if (response.isSuccessful) {
                removeExceptionFromCache(chatId)
                resetChatMutedToCategory(chatId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete chat notification setting: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAllChatExceptions(category: ChatFolderCategory): Result<Unit> {
        return try {
            val response =
                notificationSettingsApi.deleteAllChatNotificationSettings(category.name)
            
            if (response.isSuccessful) {
                val removed = chatExceptions.value.filter { category.matches(it.chatId) }
                chatExceptions.update { list -> list.filterNot { category.matches(it.chatId) } }
                removed.forEach { resetChatMutedToCategory(it.chatId) }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete chat notification settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isEnabledFor(chatId: Long): Boolean {
        val userId = currentUserId() ?: return true
        
        chatDao.isMuted(userId, chatId)?.let { return !it }
        
        val settings = notificationSettingsDao.get(userId)?.toDomain() ?: return true
        
        return settings.isEnabledFor(ChatType.fromId(chatId))
    }
    
    private fun upsertException(chatId: Long, enabled: Boolean) {
        chatExceptions.update { list ->
            listOf(ChatNotificationException(chatId, enabled)) +
                list.filterNot { it.chatId == chatId }
        }
    }
    
    private fun removeExceptionFromCache(chatId: Long) {
        chatExceptions.update { list -> list.filterNot { it.chatId == chatId } }
    }
    
    private suspend fun rollbackChatSetting(
        userId: Long,
        chatId: Long,
        muted: Boolean,
        exception: ChatNotificationException?
    ) {
        chatDao.setMuted(userId, chatId, muted)
        
        if (exception == null) {
            removeExceptionFromCache(chatId)
        } else {
            upsertException(exception.chatId, exception.enabled)
        }
    }
    
    private suspend fun resetChatMutedToCategory(chatId: Long) {
        val userId = currentUserId() ?: return
        val settings = notificationSettingsDao.get(userId)?.toDomain() ?: NotificationSettings()
        
        chatDao.setMuted(userId, chatId, !settings.isEnabledFor(ChatType.fromId(chatId)))
    }
    
    private suspend fun currentUserId(): Long? = accountDao.getCurrentAccount()?.userId
    
    private fun NotificationSettingsEntity.toDomain() = NotificationSettings(
        privateChats = privateChats,
        groups = groups,
        channels = channels
    )
    
    private fun NotificationSettings.toEntity(userId: Long) = NotificationSettingsEntity(
        userId = userId,
        privateChats = privateChats,
        groups = groups,
        channels = channels
    )
    
    private companion object {
        const val TAG = "NotificationSettings"
    }
}
