/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.channel

import android.util.Log
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.SetNoCopyRequestDto
import javax.inject.Inject

/**
 * Защита контента канала: запрет копирования текста, пересылки и сохранения медиа.
 *
 * Сервер разрешает менять флаг только владельцу канала, поэтому для остальных
 * участников вызов вернёт ошибку.
 */
class ChannelContentProtectionRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao
) {
    
    /**
     * Меняет запрет копирования у канала.
     *
     * @param channel текущее состояние канала, нужно для обновления кэша без потери
     * остальных полей.
     */
    suspend fun setNoCopy(channel: Channel, noCopy: Boolean): Result<Unit> {
        return try {
            val response = channelApi.setNoCopy(channel.id, SetNoCopyRequestDto(noCopy))
            if (response.isSuccessful) {
                channelDao.insert(channel.copy(noCopy = noCopy).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Set no copy failed: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при изменении запрета копирования канала", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "ChannelContentProtectionRepository"
    }
}
