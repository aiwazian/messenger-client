package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.AuthRequest
import com.aiwazian.messenger.data.RegisterRequest
import com.aiwazian.messenger.utils.WebSocketManager
import javax.inject.Inject

class AuthService @Inject constructor() {
    
    suspend fun logout() {
        RetrofitInstance.api.logout()
        WebSocketManager.close()
        TokenManager.setAuthorized(false)
        TokenManager.removeToken()
    }
    
    suspend fun login(authRequest: AuthRequest): String? {
        val response = RetrofitInstance.api.login(authRequest)
        
        return response.body()?.message
    }
    
    suspend fun register(registerRequest: RegisterRequest): Boolean {
        val response = RetrofitInstance.api.register(registerRequest)
        
        return response.code() == 200
    }
    
    suspend fun findUserByLogin(login: String): Boolean {
        val response = RetrofitInstance.api.findUserByLogin(login)
        return response.isSuccessful
    }
}