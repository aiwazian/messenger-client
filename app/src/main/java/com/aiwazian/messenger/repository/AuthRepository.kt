/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.AppDatabase
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.domain.ChangeLoginRequest
import com.aiwazian.messenger.domain.ChangePasswordRequest
import com.aiwazian.messenger.domain.LoginCheckResult
import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignInResponse
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toDto
import com.aiwazian.messenger.network.ApiResult
import com.aiwazian.messenger.network.api.AuthApi
import com.aiwazian.messenger.network.api.UserApi
import com.aiwazian.messenger.network.dto.ChangeLoginRequestDto
import com.aiwazian.messenger.network.dto.ChangePasswordRequestDto
import com.aiwazian.messenger.network.dto.EmailResponseDto
import com.aiwazian.messenger.network.dto.RequestPasswordResetDto
import com.aiwazian.messenger.network.dto.ResetPasswordRequestDto
import com.aiwazian.messenger.network.dto.SetEmailRequestDto
import com.aiwazian.messenger.network.dto.VerifyEmailRequestDto
import com.aiwazian.messenger.network.dto.VerifyResetCodeDto
import com.aiwazian.messenger.network.toApiError
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.SessionManager
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
    
    suspend fun getFirstAccountWithToken(): AccountEntity? {
        return try {
            accountDao.getFirstAccountWithToken()
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при получении первого аккаунта с токеном",
                e
            )
            null
        }
    }
    
    suspend fun setCurrent(userId: Long) {
        try {
            accountDao.setCurrent(userId)
        } catch (e: Exception) {
            Log.e(
                "AuthRepository",
                "Ошибка при установке текущего аккаунта",
                e
            )
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
    
    suspend fun checkLoginAvailable(login: String): Result<LoginCheckResult> {
        return try {
            val response = authApi.checkLoginAvailable(login)
            when (response.code()) {
                200 -> {
                    val body = response.body()
                    Result.success(
                        LoginCheckResult(
                            available = body?.available ?: false,
                            canReset = body?.canReset ?: false
                        )
                    )
                }
                
                409 -> Result.success(LoginCheckResult(available = false, canReset = false))
                else -> Result.success(LoginCheckResult(available = false, canReset = false))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при проверке логина", e)
            Result.failure(e)
        }
    }
    
    suspend fun signIn(request: SignInRequest): ApiResult<SignInResponse> {
        return try {
            val dto = request.toDto()
            val response = authApi.signIn(dto)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it.toDomain())
                } ?: ApiResult.Error.Unknown(IllegalStateException("Empty body"))
            } else {
                response.toApiError()
            }
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Нет интернета", e)
            ApiResult.Error.NoInternet
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Таймаут", e)
            ApiResult.Error.Timeout
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при входе", e)
            ApiResult.Error.Unknown(e)
        }
    }
    
    suspend fun signUp(request: SignUpRequest): ApiResult<SignInResponse> {
        return try {
            val dto = request.toDto()
            val response = authApi.signUp(dto)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it.toDomain())
                } ?: ApiResult.Error.Unknown(IllegalStateException("Empty body"))
            } else {
                response.toApiError()
            }
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Нет интернета", e)
            ApiResult.Error.NoInternet
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Таймаут", e)
            ApiResult.Error.Timeout
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при входе", e)
            ApiResult.Error.Unknown(e)
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
    
    suspend fun changeLogin(request: ChangeLoginRequest): Result<Unit> {
        return try {
            val dto = ChangeLoginRequestDto(login = request.login)
            val response = userApi.changeLogin(dto)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка смены логина: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при смене логина", e)
            Result.failure(e)
        }
    }
    
    suspend fun setEmail(email: String): Result<Unit> {
        return try {
            val dto = SetEmailRequestDto(email = email)
            val response = userApi.setEmail(dto)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка установки почты: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при установке почты", e)
            Result.failure(e)
        }
    }
    
    suspend fun verifyEmail(code: String): Result<EmailResponseDto> {
        return try {
            val dto = VerifyEmailRequestDto(code = code)
            val response = userApi.verifyEmail(dto)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Пустой ответ сервера"))
            } else {
                Result.failure(Exception("Неверный код"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при верификации почты", e)
            Result.failure(e)
        }
    }
    
    suspend fun disableEmail(): Result<Unit> {
        return try {
            val response = userApi.disableEmail()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка отключения почты: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при отключении почты", e)
            Result.failure(e)
        }
    }
    
    suspend fun getEmail(): Result<EmailResponseDto> {
        return try {
            val response = userApi.getEmail()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Пустой ответ сервера"))
            } else {
                Result.failure(Exception("Ошибка получения почты: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при получении почты", e)
            Result.failure(e)
        }
    }
    
    suspend fun requestPasswordReset(login: String): ApiResult<Unit> {
        return try {
            val dto = RequestPasswordResetDto(login = login)
            val response = authApi.requestPasswordReset(dto)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                response.toApiError()
            }
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Нет интернета", e)
            ApiResult.Error.NoInternet
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Таймаут", e)
            ApiResult.Error.Timeout
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при запросе сброса пароля", e)
            ApiResult.Error.Unknown(e)
        }
    }
    
    suspend fun verifyResetCode(login: String, code: String): ApiResult<Boolean> {
        return try {
            val dto = VerifyResetCodeDto(login = login, code = code)
            val response = authApi.verifyResetCode(dto)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.valid ?: false)
            } else {
                response.toApiError()
            }
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Нет интернета", e)
            ApiResult.Error.NoInternet
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Таймаут", e)
            ApiResult.Error.Timeout
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при верификации кода", e)
            ApiResult.Error.Unknown(e)
        }
    }
    
    suspend fun resetPassword(
        login: String,
        code: String,
        newPassword: String
    ): ApiResult<SignInResponse> {
        return try {
            val dto = ResetPasswordRequestDto(login = login, code = code, newPassword = newPassword)
            val response = authApi.resetPassword(dto)
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it.toDomain())
                } ?: ApiResult.Error.Unknown(IllegalStateException("Empty body"))
            } else {
                response.toApiError()
            }
        } catch (e: UnknownHostException) {
            Log.e("AuthRepository", "Нет интернета", e)
            ApiResult.Error.NoInternet
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Таймаут", e)
            ApiResult.Error.Timeout
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка при сбросе пароля", e)
            ApiResult.Error.Unknown(e)
        }
    }
}
