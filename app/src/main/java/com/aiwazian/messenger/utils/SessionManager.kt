/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
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
        Log.d("SessionManager", "Initialized with repository")
    }

    fun getToken(): String {
        return _token.value
    }

    fun isAuthorized(): Boolean = _isAuthorized

    fun setAuthorized(value: Boolean) {
        _isAuthorized = value
    }

    suspend fun loadSession() {
        val currentToken = authRepository?.getCurrentToken() ?: ""
        _token.update { currentToken }
        _isAuthorized = currentToken.isNotEmpty()
        isInit = true
    }

    suspend fun saveSession(
        userId: Long,
        token: String
    ) {
        _token.update { token }
        _isAuthorized = token.isNotEmpty()

        val repo = authRepository
            ?: throw IllegalStateException("AuthRepository not initialized. Call init() first.")
        
        repo.saveAccount(
            AccountEntity(
                id = userId,
                isCurrent = true,
                token = token
            )
        )
        Log.d("SessionManager", "Saved session: userId=$userId, token=${if (token.isNotEmpty()) "present" else "empty"}")
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
