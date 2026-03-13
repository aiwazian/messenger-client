package com.aiwazian.messenger.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

object TokenManager {
    
    private val _token = MutableStateFlow("")
    
    private var _isAuthorized = false
    
    private var unauthorizedCallback: (() -> Unit)? = null
    
    fun getToken() = _token.value
    
    fun setAuthorized(value: Boolean) {
        _isAuthorized = value
    }
    
    fun isAuthorized() = _isAuthorized
    
    suspend fun init() {
        val dataStore = DataStoreManager.getInstance()
        _token.update { dataStore.getToken().first() }
    }
    
    suspend fun saveToken(token: String) {
        _token.update { token }
        val dataStore = DataStoreManager.getInstance()
        dataStore.saveToken(token)
    }
    
    suspend fun removeToken() {
        _token.update { "" }
        val dataStore = DataStoreManager.getInstance()
        dataStore.saveToken("")
    }
    
    fun setUnauthorizedCallback(callback: () -> Unit) {
        unauthorizedCallback = callback
    }
    
    fun getUnauthorizedCallback() = unauthorizedCallback
}