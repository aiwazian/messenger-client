/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toUpdateRequest
import com.aiwazian.messenger.network.api.UserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao
) {
    fun getMe(): Flow<User> = userDao.getMe().filterNotNull().map {
        it.toDomain()
    }.onStart {
        userDao.getMe().first()?.let {
            try {
                val response = userApi.getMe()
                if (response.isSuccessful) {
                    response.body()?.let { userDao.insert(it.toEntity()) }
                }
            } catch (_: Exception) {
            }
        }
    }
    
    fun getById(id: Long): Flow<User> = flow {
        val localUser = userDao.get(id)
        if (localUser != null) {
            emit(localUser.toDomain())
        }
        
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
}
