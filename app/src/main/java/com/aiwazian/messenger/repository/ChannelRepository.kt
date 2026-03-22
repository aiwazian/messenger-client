/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.CreateChannelRequestDto
import com.aiwazian.messenger.network.dto.UpdateChannelRequestDto
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao
) {
    
    fun getById(id: Long): Flow<Channel> = flow {
        coroutineScope {
            launch {
                try {
                    val response = channelApi.getChannelById(id)
                    if (response.isSuccessful) {
                        val dto = response.body()
                        if (dto != null) {
                            val channel = dto.toDomain()
                            channelDao.insert(channel.toEntity())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "ChannelRepository",
                        "Ошибка при получении канала по сети",
                        e
                    )
                }
            }
            
            emitAll(getByIdFlow(id))
        }
    }

    fun getByIdFlow(id: Long): Flow<Channel> {
        return channelDao.getFlow(id)
            .filterNotNull()
            .map { it.toDomain() }
    }
    
    suspend fun create(channelInfo: Channel): Result<Long> {
        return try {
            val request = CreateChannelRequestDto(
                name = channelInfo.name,
                bio = channelInfo.bio,
                channelType = ChannelType.entries.getOrElse(channelInfo.channelType) { ChannelType.PRIVATE },
                username = channelInfo.username
            )
            val response = channelApi.createChannel(request)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val channel = dto.toDomain()
                    channelDao.insert(channel.toEntity())
                    Result.success(channel.id)
                } else {
                    Result.failure(Exception("No channel returned"))
                }
            } else {
                Result.failure(Exception("Create failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при создании канала",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun update(channel: Channel): Result<Unit> {
        return try {
            val request = UpdateChannelRequestDto(
                name = channel.name,
                bio = channel.bio,
                channelType = ChannelType.entries.getOrNull(channel.channelType),
                username = channel.username
            )
            val response =
                channelApi.updateChannel(
                    channel.id,
                    request
                )
            if (response.isSuccessful) {
                channelDao.insert(channel.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при обновлении канала",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun delete(id: Long): Result<Unit> {
        return try {
            val response = channelApi.deleteChannel(id)
            if (response.isSuccessful) {
                channelDao.delete(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при удалении канала",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun getSubscribers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response =
                channelApi.getChannelSubscribers(
                    id,
                    skip,
                    take,
                    search
                )
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при получении подписчиков канала",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun join(id: Long): Result<Unit> {
        return try {
            val response = channelApi.joinChannel(id)
            if (response.isSuccessful) {
                val channel = channelDao.get(id)
                if (channel != null) {
                    channelDao.update(channel.copy(isSubscribed = true))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при подписке на канал",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun leave(id: Long): Result<Unit> {
        return try {
            val response = channelApi.leaveChannel(id)
            if (response.isSuccessful) {
                val channel = channelDao.get(id)
                if (channel != null) {
                    channelDao.update(channel.copy(isSubscribed = false))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при отписке от канала",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun save(channel: Channel): Long? {
        return try {
            val request = UpdateChannelRequestDto(
                name = channel.name,
                bio = channel.bio,
                channelType = ChannelType.fromInt(channel.channelType),
                username = channel.username
            )
            val response =
                channelApi.updateChannel(
                    channel.id,
                    request
                )
            if (response.isSuccessful) {
                channelDao.insert(channel.toEntity())
                channel.id
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при сохранении канала",
                e
            )
            null
        }
    }
    
    suspend fun kickUser(
        channelId: Long,
        userId: Long
    ): Result<Unit> {
        return try {
            val response =
                channelApi.kickUser(
                    channelId,
                    userId
                )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Kick failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при выгонении пользователя",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun banUser(
        channelId: Long,
        userId: Long
    ): Result<Unit> {
        return try {
            val response =
                channelApi.banUser(
                    channelId,
                    userId
                )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ban failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при бане пользователя",
                e
            )
            Result.failure(e)
        }
    }
}
