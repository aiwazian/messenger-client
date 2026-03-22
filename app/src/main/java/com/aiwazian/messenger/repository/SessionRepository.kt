/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.network.api.SessionApi
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.domain.Session
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val sessionApi: SessionApi
) {

    suspend fun getAllSessions(): List<Session> {
        return try {
            val response = sessionApi.getAllSessions()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                dtos.map { it.toDomain() }
            } else {
                Log.e("SessionRepository", "Failed to get sessions: ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting sessions", e)
            emptyList()
        }
    }

    suspend fun getDeviceCount(): Int {
        return try {
            val sessions = getAllSessions()
            sessions.size
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting device count", e)
            1
        }
    }

    suspend fun deleteSession(sessionId: Int): Boolean {
        return try {
            val response = sessionApi.deleteSession(sessionId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting session", e)
            false
        }
    }

    suspend fun deleteAllSessions(): Boolean {
        return try {
            val response = sessionApi.deleteAllSessions()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting all sessions", e)
            false
        }
    }
}
