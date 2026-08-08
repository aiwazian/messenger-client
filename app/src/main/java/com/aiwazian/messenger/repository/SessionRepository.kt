/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.domain.Session
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.SessionApi
import com.aiwazian.messenger.network.dto.UpdateInstallationIdDto
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val sessionApi: SessionApi
) {
    
    suspend fun getAllSessions(): Result<List<Session>> {
        return try {
            val response = sessionApi.getAllSessions()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty().map { it.toDomain() }
                Result.success(dtos)
            } else {
                Log.e("SessionRepository", "Failed to get sessions: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting sessions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Привязывает Firebase Installation ID к текущей сессии: в новом API FCM
     * именно FID, а не registration token, является адресатом уведомления.
     */
    suspend fun updateInstallationId(installationId: String): Result<Unit> {
        return try {
            val response = sessionApi.updateInstallationId(UpdateInstallationIdDto(installationId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error updating installation id", e)
            Result.failure(e)
        }
    }
    
    suspend fun getDeviceCount(): Result<Int> {
        return try {
            val response = sessionApi.getAllSessions()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty().map { it.toDomain() }
                Result.success(dtos.size)
            } else {
                Log.e("SessionRepository", "Failed to get sessions: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting device count", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteSession(sessionId: Int): Result<Unit> {
        return try {
            val response = sessionApi.deleteSession(sessionId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Throwable("Error while delete session: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting session", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteAllSessions(): Result<Unit> {
        return try {
            val response = sessionApi.deleteAllSessions()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting all sessions", e)
            Result.failure(e)
        }
    }
}
