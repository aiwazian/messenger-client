/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.NotificationSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingsDao {
    
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    fun observe(userId: Long): Flow<NotificationSettingsEntity?>
    
    /**
     * Разовое чтение для проверки перед показом уведомления.
     *
     * Подписка тут не нужна: решение принимается один раз в момент прихода сообщения.
     */
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: Long): NotificationSettingsEntity?
    
    @Upsert
    suspend fun upsert(settings: NotificationSettingsEntity)
    
    @Query("DELETE FROM notification_settings")
    suspend fun deleteAll()
}
