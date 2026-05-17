/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.domain.ChangePasswordRequest
import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignInResponse
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toDto
import com.aiwazian.messenger.network.api.AuthApi
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.network.dto.ChangePasswordRequestDto
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.SessionManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val accountDao: AccountDao,
    private val database: AppDatabase,
    private val dataStoreManager: DataStoreManager
) {
    
    suspend fun deleteMe(): Result<Unit> {
        return try {
            val response = userApi.deleteMe()
            if (response.isSuccessful) {
                database.clearAllTables()
                dataStoreManager.clear()
                SessionManager.logout()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка удаления аккаунта: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при удалении аккаунта",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentToken(): String? {
        return try {
            val token = accountDao.getCurrentToken()
            token
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при получении токена: ${e.message}",
                e
            )
            null
        }
    }
    
    suspend fun getAllAccounts(): List<AccountEntity> {
        return try {
            accountDao.getAllAccounts()
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при получении всех аккаунтов",
                e
            )
            emptyList()
        }
    }
    
    suspend fun saveAccount(account: AccountEntity) {
        try {
            val existingAccount = accountDao.getById(account.userId)
            
            if (existingAccount == null) {
                if (account.isCurrent) {
                    accountDao.resetCurrent()
                }
                accountDao.add(account)
                Log.d(
                    "AuthRepository",
                    "saveAccount: ADDED new account"
                )
            } else {
                accountDao.update(account)
                Log.d(
                    "AuthRepository",
                    "saveAccount: UPDATED existing account"
                )
            }
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при сохранении аккаунта: ${e.message}",
                e
            )
        }
    }
    
    suspend fun switchAccount(id: Long) {
        try {
            accountDao.resetCurrent()
            accountDao.setCurrent(id)
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при переключении аккаунта",
                e
            )
        }
    }
    
    suspend fun getCurrentAccountCreatedAt(): Long {
        return try {
            val accounts = accountDao.getAllAccounts()
            val currentAccount = accounts.find { it.isCurrent }
            currentAccount?.createdAt ?: 0L
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при получении времени создания сессии",
                e
            )
            0L
        }
    }
    
    suspend fun clearCurrentToken() {
        try {
            accountDao.deleteCurrent()
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при очистке токена",
                e
            )
        }
    }
    
    suspend fun checkLoginAvailable(login: String): Result<Boolean> {
        return try {
            val response = authApi.checkLoginAvailable(login)
            when (response.code()) {
                200 -> Result.success(true)
                409 -> Result.success(false)
                else -> Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при проверке логина", e)
            Result.failure(e)
        }
    }
    
    suspend fun signIn(request: SignInRequest): Result<SignInResponse> {
        return try {
            val dto = request.toDto()
            val response = authApi.signIn(dto)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.toDomain())
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при входе", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUp(request: SignUpRequest): Result<SignInResponse> {
        return try {
            val dto = request.toDto()
            val response = authApi.signUp(dto)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.toDomain())
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при регистрации", e)
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            clearCurrentToken()
            val response = authApi.logout()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка выхода: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при выходе", e)
            Result.failure(e)
        }
    }
    
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return try {
            val dto = ChangePasswordRequestDto(password = request.password)
            val response = userApi.changePassword(dto)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка смены пароля: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при смене пароля", e)
            Result.failure(e)
        }
    }
}
