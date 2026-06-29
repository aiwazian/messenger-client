/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.database.entity.AvatarEntity
import com.aiwazian.messenger.domain.OwnedChannel
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toUpdateRequest
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.SetProfileChannelRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val avatarDao: AvatarDao
) {
    fun getMe(): Flow<User> = userDao.getMeWithAvatars().filterNotNull().map { userWithAvatars ->
        val avatars =
            userWithAvatars.avatars.sortedBy { it.avatar.sortOrder }.map { avatarWithFile ->
                val uri = if (!avatarWithFile.file?.path.isNullOrBlank()) {
                    avatarWithFile.file.path.toUri()
                } else {
                    null
                }
                avatarWithFile.avatar.toDomain(uri)
            }
        
        userWithAvatars.user.toDomain(avatars)
    }
    
    suspend fun fetchMe() {
        try {
            val response = userApi.getMe()
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    userDao.insert(user.toEntity())
                    
                    val avatars = user.avatars.map { it.toEntity(user.id) }
                    avatarDao.insertAvatars(avatars)
                    
                    if (user.avatars.isEmpty()) {
                        avatarDao.deleteAvatarsByUserId(user.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при загрузке профиля в onStart", e)
        }
    }
    
    fun getById(id: Long): Flow<User> = getByIdOrNull(id).filterNotNull()
    
    fun getByIdOrNull(id: Long): Flow<User?> =
        userDao.getWithAvatarsFlow(id).map { userWithAvatars ->
            userWithAvatars ?: return@map null
            val avatars =
                userWithAvatars.avatars.sortedBy { it.avatar.sortOrder }.map { avatarWithFile ->
                    val uri = if (!avatarWithFile.file?.path.isNullOrBlank()) {
                        avatarWithFile.file.path.toUri()
                    } else {
                        null
                    }
                    avatarWithFile.avatar.toDomain(uri)
                }
            userWithAvatars.user.toDomain(avatars)
        }
    
    suspend fun fetchById(id: Long) {
        try {
            val response = userApi.getUserById(id)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val user = dto.toDomain()
                    userDao.insert(user.toEntity())
                    
                    val avatars = dto.avatars.map { it.toEntity(user.id) }
                    avatarDao.insertAvatars(avatars)
                    
                    if (dto.avatars.isEmpty()) {
                        avatarDao.deleteAvatarsByUserId(user.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при получении профиля", e)
        }
    }
    
    suspend fun updateProfile(user: User): Result<Unit> {
        return try {
            val request = user.toUpdateRequest()
            val response = userApi.updateMe(request)
            if (response.isSuccessful) {
                userDao.insert(user.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при обновлении профиля", e)
            Result.failure(e)
        }
    }
    
    suspend fun saveUsername(username: String): Result<Unit> {
        return try {
            val currentUser =
                getMe().firstOrNull() ?: return Result.failure(Exception("User not initialized"))
            val updatedUser = currentUser.copy(username = username.ifBlank { null })
            val request = updatedUser.toUpdateRequest()
            val response = userApi.updateMe(request)
            if (response.isSuccessful) {
                userDao.insert(updatedUser.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при сохранении username", e)
            Result.failure(e)
        }
    }
    
    suspend fun initUploadAvatar(
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): Result<FileInitResponseDto> {
        return try {
            val requestBody = FileInitRequestDto(fileName, fileSize, mimeType)
            val response = userApi.initUploadAvatar(requestBody)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при удалении аватара", e)
            Result.failure(e)
        }
    }
    
    suspend fun confirmUploadAvatar(fileId: String): Result<Unit> {
        return try {
            val response = userApi.confirmUploadAvatar(fileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upload failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAvatarDownloadUrl(fileId: String): Result<String> {
        return try {
            val response = userApi.getAvatarDownloadUrl(fileId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.downloadUrl)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteAvatar(fileId: String): Result<Unit> {
        return try {
            val response = userApi.deleteAvatar(fileId)
            if (response.isSuccessful) {
                avatarDao.deleteAvatarByFileId(fileId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при удалении аватара", e)
            Result.failure(e)
        }
    }
    
    suspend fun addAvatarLocal(fileId: String) {
        val user = userDao.getMe().firstOrNull()
        if (user != null) {
            val currentAvatars = avatarDao.getAvatarsByUserId(user.id)
            val newSortOrder = (currentAvatars.maxOfOrNull { it.sortOrder } ?: 0) + 1
            
            avatarDao.insertAvatars(
                listOf(
                    AvatarEntity(
                        fileId = fileId,
                        userId = user.id,
                        sortOrder = newSortOrder
                    )
                )
            )
        }
    }
    
    suspend fun setProfileChannel(channelId: Long): Result<Unit> {
        return try {
            val response = userApi.setProfileChannel(
                SetProfileChannelRequestDto(channelId = channelId.toString())
            )
            if (response.isSuccessful) {
                val user = getMe().firstOrNull()
                if (user != null) {
                    userDao.insert(user.copy(profileChannelId = channelId).toEntity())
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to set profile channel"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error setting profile channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeProfileChannel(): Result<Unit> {
        return try {
            val response = userApi.removeProfileChannel()
            if (response.isSuccessful) {
                val user = getMe().firstOrNull()
                if (user != null) {
                    userDao.insert(user.copy(profileChannelId = null).toEntity())
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove profile channel"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing profile channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun getOwnedChannels(): Result<List<OwnedChannel>> {
        return try {
            val response = userApi.getOwnedChannels()
            if (response.isSuccessful) {
                Result.success(response.body()?.map { it.toDomain() }.orEmpty())
            } else {
                Result.failure(Exception("Failed to get owned channels"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting owned channels", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBlockedUsers(): Result<List<User>> {
        return try {
            val response = userApi.getBlockedUsers()
            if (response.isSuccessful) {
                Result.success(response.body()?.map { it.toDomain() }.orEmpty())
            } else {
                Result.failure(Exception("Failed to get blocked users"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting blocked users", e)
            Result.failure(e)
        }
    }
    
    suspend fun blockUser(userId: Long): Result<Unit> {
        return try {
            val response = userApi.blockUser(userId)
            if (response.isSuccessful) {
                userDao.getById(userId)?.let {
                    userDao.insert(it.copy(isBlocked = true))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Block user failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unblockUser(userId: Long): Result<Unit> {
        return try {
            val response = userApi.unblockUser(userId)
            if (response.isSuccessful) {
                userDao.getById(userId)?.let {
                    userDao.insert(it.copy(isBlocked = false))
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unblock user failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
