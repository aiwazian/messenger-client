package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.SessionInfo
import javax.inject.Inject

class SessionService @Inject constructor() {
    
    suspend fun terminateSession(sessionId: Int): Boolean {
        val response = RetrofitInstance.api.terminateSession(sessionId)
        return response.isSuccessful
    }
    
    suspend fun terminateAllSessions(): Boolean {
        val response = RetrofitInstance.api.terminateAllSessions()
        return response.isSuccessful
    }
    
    suspend fun getSessions(): List<SessionInfo>? {
        val response = RetrofitInstance.api.getSessions()
        
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        
        return null
    }
}