/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.network.dto.ChangePasswordRequestDto
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toUpdateRequest
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.database.dao.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) {

    fun getMe(): Flow<User> = flow {
        try {
            val response = userApi.getMe()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val user = dto.toDomain()
                    val userEntity = user.toEntity()
                    userDao.insert(userEntity)

                    emit(user)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при запросе Get Me", e)
        }
    }

    fun getById(id: Long): Flow<User> = flow {
        try {
            val response = userApi.getUserById(id)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val user = dto.toDomain()
                    userDao.insert(user.toEntity())
                    emit(user)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при получении профиля", e)
        }

        val localUser = userDao.get(id)
        if (localUser != null) {
            emit(localUser.toDomain())
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
            val currentUser = userDao.getMe() ?: return false
            val updatedUser = currentUser.toDomain().copy(username = username.ifEmpty { null })
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
}
