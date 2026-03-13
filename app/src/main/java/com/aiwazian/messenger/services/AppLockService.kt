package com.aiwazian.messenger.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockService @Inject constructor() {
    
    private val _isLockApp = MutableStateFlow(false)
    val isLockApp = _isLockApp.asStateFlow()
    
    private val _passcode = MutableStateFlow("")
    
    private val _hasPasscode = MutableStateFlow(false)
    val hasPasscode = _hasPasscode.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val dataStoreManager = DataStoreManager.getInstance()
    
    init {
        coroutineScope.launch {
            val passcode = dataStoreManager.getPasscode().first()
            _passcode.update { passcode }
            
            _hasPasscode.update { _passcode.value.isNotBlank() }
            
            val isLock = dataStoreManager.getIsLockApp().first()
            _isLockApp.update { isLock }
        }
    }
    
    suspend fun lock() {
        _isLockApp.update { true }
        dataStoreManager.saveIsLockApp(true)
    }
    
    suspend fun unlock() {
        _isLockApp.update { false }
        dataStoreManager.saveIsLockApp(false)
    }
    
    suspend fun disablePasscode() {
        _hasPasscode.update { false }
        dataStoreManager.savePasscode("")
    }
    
    suspend fun changePasscode(newPasscode: String) {
        _passcode.update { newPasscode }
        _hasPasscode.update { true }
        dataStoreManager.savePasscode(newPasscode)
    }
    
    fun checkPasscode(passcode: String): Boolean {
        return passcode == _passcode.value
    }
}