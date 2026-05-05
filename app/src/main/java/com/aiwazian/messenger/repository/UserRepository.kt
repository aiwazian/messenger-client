/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toUpdateRequest
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    }.onStart {
        val localUser = userDao.getMe().first()
        if (localUser == null) {
            try {
                val response = userApi.getMe()
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        userDao.insert(user.toEntity())
                        
                        val avatars = user.avatars.map { it.toEntity(user.id) }
                        avatarDao.insertAvatars(avatars)
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Ошибка при загрузке профиля при старте", e)
            }
        }
    }
    
    fun getById(id: Long): Flow<User> =
        userDao.getWithAvatarsFlow(id).filterNotNull().map { userWithAvatars ->
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
        }.onStart {
            try {
                val response = userApi.getUserById(id)
                if (response.isSuccessful) {
                    response.body()?.let { dto ->
                        val user = dto.toDomain()
                        userDao.insert(user.toEntity())
                        
                        val avatars = dto.avatars.map { it.toEntity(user.id) }
                        avatarDao.insertAvatars(avatars)
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
    
    suspend fun saveUsername(username: String): Boolean {
        return try {
            val currentUser = getMe().firstOrNull() ?: return false
            val updatedUser = currentUser.copy(username = username.ifEmpty { null })
            val request = updatedUser.toUpdateRequest()
            val response = userApi.updateMe(request)
            if (response.isSuccessful) {
                userDao.insert(updatedUser.toEntity())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при сохранении username", e)
            false
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
                    throw Exception("Empty body")
                }
            } else {
                throw Exception("Unsuccessful request")
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
                    throw Exception("Empty body")
                }
            } else {
                throw Exception("Unsuccessful request: ${response.errorBody()}")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
}
