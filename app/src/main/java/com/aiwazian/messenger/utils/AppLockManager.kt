/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

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
class AppLockManager @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    
    private val _isLockApp = MutableStateFlow(false)
    val isLockApp = _isLockApp.asStateFlow()
    
    private val _passcode = MutableStateFlow("")
    
    private val _hasPasscode = MutableStateFlow(false)
    val hasPasscode = _hasPasscode.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)

    private val _blockedUntil = MutableStateFlow(0L)
    val blockedUntil = _blockedUntil.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            val passcode = dataStoreManager.getPasscode().first()
            _passcode.update { passcode }

            _hasPasscode.update { _passcode.value.isNotBlank() }

            val isLock = dataStoreManager.getIsLockApp().first()
            _isLockApp.update { isLock }

            _failedAttempts.update { dataStoreManager.getFailedAttempts().first() }
            _blockedUntil.update { dataStoreManager.getBlockedUntil().first() }
        }
    }

    suspend fun lock() {
        _isLockApp.update { true }
        dataStoreManager.saveIsLockApp(true)
    }

    suspend fun unlock() {
        _isLockApp.update { false }
        dataStoreManager.saveIsLockApp(false)
        resetFailedAttempts()
    }

    suspend fun incrementFailedAttempts() {
        val newAttempts = _failedAttempts.value + 1
        _failedAttempts.update { newAttempts }
        dataStoreManager.saveFailedAttempts(newAttempts)

        if (newAttempts >= 5) {
            val blockTime = System.currentTimeMillis() + 30_000
            _blockedUntil.update { blockTime }
            dataStoreManager.saveBlockedUntil(blockTime)
        }
    }

    suspend fun resetFailedAttempts() {
        _failedAttempts.update { 0 }
        _blockedUntil.update { 0L }
        dataStoreManager.saveFailedAttempts(0)
        dataStoreManager.saveBlockedUntil(0L)
    }

    suspend fun disablePasscode() {
        _hasPasscode.update { false }
        dataStoreManager.savePasscode("")
        resetFailedAttempts()
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
