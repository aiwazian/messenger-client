/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.SetNoCopyRequestDto
import javax.inject.Inject

/**
 * Управляет запретом копирования контента для каналов и групп.
 *
 * Логика вынесена в отдельный класс, чтобы не увеличивать и без того большие
 * [ChannelRepository] и [GroupRepository]. Сервер разрешает изменять флаг только
 * владельцу, поэтому для остальных участников вызов вернёт ошибку.
 */
class NoCopyRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val groupApi: GroupApi,
    private val channelDao: ChannelDao,
    private val groupDao: GroupDao
) {
    
    /**
     * Меняет запрет копирования у канала.
     *
     * @param channel текущее состояние канала, нужно для обновления кэша без потери
     * остальных полей.
     */
    suspend fun setChannelNoCopy(channel: Channel, noCopy: Boolean): Result<Unit> {
        return try {
            val response = channelApi.setNoCopy(channel.id, SetNoCopyRequestDto(noCopy))
            if (response.isSuccessful) {
                channelDao.insert(channel.copy(noCopy = noCopy).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Set no copy failed: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("NoCopyRepository", "Ошибка при изменении запрета копирования канала", e)
            Result.failure(e)
        }
    }
    
    /**
     * Меняет запрет копирования у группы.
     *
     * @param group текущее состояние группы, нужно для обновления кэша без потери
     * остальных полей.
     */
    suspend fun setGroupNoCopy(group: Group, noCopy: Boolean): Result<Unit> {
        return try {
            val response = groupApi.setNoCopy(group.id, SetNoCopyRequestDto(noCopy))
            if (response.isSuccessful) {
                groupDao.insert(group.copy(noCopy = noCopy).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Set no copy failed: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("NoCopyRepository", "Ошибка при изменении запрета копирования группы", e)
            Result.failure(e)
        }
    }
}
