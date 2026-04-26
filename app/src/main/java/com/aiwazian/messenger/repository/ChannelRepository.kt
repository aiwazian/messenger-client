/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.CreateChannelRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.UpdateChannelRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao,
    private val chatDao: ChatDao
) {
    
    suspend fun create(name: String, bio: String): Result<Long> {
        return try {
            val response = channelApi.createChannel(CreateChannelRequestDto(name, bio))
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
                Result.failure(Exception("Create channel failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error creating channel", e)
            Result.failure(e)
        }
    }
    
    fun getById(channelId: Long): Flow<Channel> = flow {
        val localChannel = channelDao.get(channelId)
        if (localChannel != null) {
            emit(localChannel.toDomain())
        }
        
        try {
            val response = channelApi.getChannelById(channelId)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val channel = dto.toDomain()
                    channelDao.insert(channel.toEntity())
                    emit(channel)
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error getting channel", e)
        }
    }
    
    fun getByIdFlow(channelId: Long): Flow<Channel> = channelDao.getFlow(channelId)
        .mapNotNull { it?.toDomain() }
    
    suspend fun getSubscribers(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = channelApi.getChannelSubscribers(channelId, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception(""))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error getting subscribers", e)
            Result.failure(e)
        }
    }
    
    suspend fun update(channel: Channel): Result<Unit> {
        return try {
            val request = UpdateChannelRequestDto(
                name = channel.name,
                bio = channel.bio,
                channelType = channel.channelType,
                username = channel.username
            )
            val response = channelApi.updateChannel(channel.id, request)
            if (response.isSuccessful) {
                channelDao.insert(channel.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error updating channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateChannelType(
        channelId: Long,
        channelType: ChannelType,
        username: String?
    ): Result<Unit> {
        return try {
            val request = UpdateChannelRequestDto(channelType = channelType, username = username)
            val response = channelApi.updateChannel(channelId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    channelDao.insert(body.toDomain().toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Update failed"))
                }
            } else {
                Result.failure(Exception("Update failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error updating channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun delete(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.deleteChannel(channelId)
            if (response.isSuccessful) {
                channelDao.delete(channelId)
                chatDao.deleteChat(channelId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error deleting channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun join(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.joinChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error joining channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun leave(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.leaveChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error leaving channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun kickUser(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.kickUser(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Kick failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error kicking user", e)
            Result.failure(e)
        }
    }
    
    suspend fun getInviteLinks(channelId: Long): Result<List<InviteLink>> {
        return try {
            val response = channelApi.getInviteLinks(channelId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get invite links"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error getting invite links", e)
            Result.failure(e)
        }
    }
    
    suspend fun createInviteLink(
        channelId: Long,
        maxUses: Int?,
        expiresInSeconds: Int? = null
    ): Result<InviteLink> {
        return try {
            val request = CreateInviteLinkRequestDto(
                maxUses = maxUses,
                expiresInSeconds = expiresInSeconds
            )
            val response = channelApi.createInviteLink(channelId, request)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.failure(Exception("No invite link returned"))
                }
            } else {
                Result.failure(Exception("Create invite link failed"))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error creating invite link", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBannedUsers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response =
                channelApi.getBannedUsers(
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
                "Ошибка при получении заблокированных пользователей",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun unbanUser(
        channelId: Long,
        userId: Long
    ): Result<Unit> {
        return try {
            val response =
                channelApi.unbanUser(
                    channelId,
                    userId
                )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unban failed"))
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при разблокировке пользователя",
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
            val response = channelApi.banUser(
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
                "Ошибка при блокировке пользователя",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun isUserBanned(channelId: Long): Result<Boolean> {
        return try {
            val response = channelApi.isUserBanned(channelId)
            if (response.isSuccessful) {
                Result.success(response.body()?.isBanned ?: false)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(
                "ChannelRepository",
                "Ошибка при проверке блокировки пользователя",
                e
            )
            Result.failure(e)
        }
    }
}
