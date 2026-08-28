/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.channel

import android.util.Log
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.domain.AvatarNotFoundException
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.mappers.toChannelEntity
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toDomainAvatars
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.CreateChannelRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.UpdateChannelRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Создание, чтение, обновление и удаление канала, а также его аватары.
 *
 * Работа с участниками вынесена в [ChannelMembersRepository], а запрет копирования
 * в [ChannelContentProtectionRepository].
 */
class ChannelCrudRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao,
    private val avatarDao: AvatarDao
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
            Log.e(TAG, "Error creating channel", e)
            Result.failure(e)
        }
    }
    
    fun getById(channelId: Long): Flow<Channel> = getByIdOrNull(channelId).filterNotNull()
    
    fun getByIdOrNull(channelId: Long): Flow<Channel?> =
        channelDao.getWithAvatarsFlow(channelId).map { channelWithAvatars ->
            channelWithAvatars ?: return@map null
            channelWithAvatars.channel.toDomain(channelWithAvatars.avatars.toDomainAvatars())
        }
    
    suspend fun fetchById(channelId: Long) {
        try {
            val response = channelApi.getChannelById(channelId)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val channel = dto.toDomain()
                    channelDao.insert(channel.toEntity())
                    
                    val avatars = dto.avatars.map { it.toChannelEntity(channel.id) }
                    
                    /*
                     * Сервер отдал полный список аватарок: пропавшие уберёт сам DAO, а
                     * пустой список — это «аватарок больше нет» и требует явной чистки.
                     */
                    if (avatars.isEmpty()) {
                        avatarDao.deleteAvatarsByChannelId(channel.id)
                    } else {
                        avatarDao.insertAvatars(avatars)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting channel in onStart", e)
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
            Log.e(TAG, "Error updating channel", e)
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
            Log.e(TAG, "Error updating channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun delete(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.deleteChannel(channelId)
            if (response.isSuccessful) {
                channelDao.delete(channelId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun initUploadAvatar(
        channelId: Long,
        name: String,
        size: Long,
        mimeType: String
    ): Result<FileInitResponseDto> {
        return try {
            val response = channelApi.initUploadAvatar(
                channelId,
                FileInitRequestDto(name, size, mimeType)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Init upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmUploadAvatar(channelId: Long, fileId: String): Result<Unit> {
        return try {
            val response = channelApi.confirmUploadAvatar(channelId, fileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Confirm upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAvatar(channelId: Long, fileId: String): Result<Unit> {
        return try {
            val response = channelApi.deleteAvatar(channelId, fileId)
            if (response.isSuccessful) {
                avatarDao.deleteAvatarByFileId(fileId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete avatar failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvatarDownloadUrl(fileId: String): Result<String> {
        return try {
            val response = channelApi.getAvatarDownloadUrl(fileId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.downloadUrl)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else if (response.code() == 404 || response.code() == 410) {
                /* Файла больше нет: DownloadAvatarUseCase уберёт аватарку из Room. */
                Result.failure(AvatarNotFoundException(fileId))
            } else {
                Result.failure(Exception("Unsuccessful request: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "ChannelCrudRepository"
    }
}
