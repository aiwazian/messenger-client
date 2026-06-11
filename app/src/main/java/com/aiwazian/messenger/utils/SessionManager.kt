/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SessionManager {

    private val _token = MutableStateFlow("")
    val token = _token.asStateFlow()

    private var _isAuthorized = false
    private var unauthorizedCallback: (() -> Unit)? = null
    private var authRepository: AuthRepository? = null

    var isInit = false
        private set

    fun init(repository: AuthRepository) {
        authRepository = repository
        isInit = true
    }

    fun getToken(): String {
        return _token.value
    }

    fun setAuthorized(value: Boolean) {
        _isAuthorized = value
    }

    suspend fun loadSession() {
        var currentToken = authRepository?.getCurrentToken() ?: ""
        
        if (currentToken.isEmpty()) {
            val fallback = authRepository?.getFirstAccountWithToken()
            if (fallback != null) {
                authRepository?.setCurrent(fallback.userId)
                currentToken = fallback.token
            }
        }

        _token.update { currentToken }
        _isAuthorized = currentToken.isNotEmpty()
        isInit = true
    }

    suspend fun saveSession(
        userId: Long,
        token: String,
        createdAt: Long
    ) {
        _token.update { token }
        _isAuthorized = token.isNotEmpty()

        val repository = authRepository
            ?: throw IllegalStateException("AuthRepository not initialized. Call init() first.")
        
        repository.saveAccount(
            AccountEntity(
                userId = userId,
                isCurrent = true,
                token = token,
                createdAt = createdAt
            )
        )
    }
    
    suspend fun logout() {
        _token.update { "" }
        _isAuthorized = false
        authRepository?.clearCurrentToken()
    }
    
    suspend fun hasAnySession(): Boolean {
        val accounts = authRepository?.getAllAccounts() ?: emptyList()
        return accounts.any { it.token.isNotEmpty() }
    }
    
    suspend fun switchAccount(userId: Long) {
        authRepository?.switchAccount(userId)
        loadSession()
    }
    
    fun setUnauthorizedCallback(callback: () -> Unit) {
        unauthorizedCallback = callback
    }
    
    fun getUnauthorizedCallback() = unauthorizedCallback
}
