/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.NotificationSettingsDao
import com.aiwazian.messenger.database.entity.NotificationSettingsEntity
import com.aiwazian.messenger.domain.NotificationSettings
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.network.api.NotificationSettingsApi
import com.aiwazian.messenger.network.dto.UpdateNotificationSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Настройки уведомлений: Room как кэш, сервер как источник правды.
 *
 * Singleton не ради состояния, а ради единого экземпляра для экрана, слушателя сокета
 * и показа уведомлений.
 */
@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val notificationSettingsApi: NotificationSettingsApi,
    private val notificationSettingsDao: NotificationSettingsDao,
    private val accountDao: AccountDao
) {
    
    /**
     * Текущие настройки активного аккаунта.
     *
     * Нет записи в базе — всё включено: это же состояние по умолчанию и на сервере.
     */
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
    
    /** Подтянуть серверное состояние в кэш — настройку могли менять с другого устройства. */
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
    
    /**
     * Сохранить новое состояние переключателей.
     *
     * Сначала пишется в Room, и только потом уходит запрос: переключатель должен
     * срабатывать сразу, а не ждать сеть. Если запрос не ушёл, значение возвращается
     * обратно: иначе локально уведомления выключены, а сервер о этом не знает и
     * продолжает слать пуши.
     */
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
    
    /** Настройку поменяли в другой сессии — событие пришло по сокету. */
    suspend fun applyRemote(settings: NotificationSettings) {
        val userId = currentUserId() ?: return
        notificationSettingsDao.upsert(settings.toEntity(userId))
    }
    
    /**
     * Показывать ли уведомление из этого чата.
     *
     * Проверка дублирует серверную не от недоверия, а потому что между выключением
     * переключателя и его доездом до сервера есть окно, да и пуш мог уйти в полёт
     * раньше. Лучше отбросить его здесь, чем показать вопреки настройке.
     *
     * Любая неопределённость толкуется в пользу показа: пропущенное уведомление
     * не вернёшь, а лишнее пользователь смахнёт.
     */
    suspend fun isEnabledFor(chatId: Long): Boolean {
        val userId = currentUserId() ?: return true
        val settings = notificationSettingsDao.get(userId)?.toDomain() ?: return true
        
        return settings.isEnabledFor(ChatType.fromId(chatId))
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
