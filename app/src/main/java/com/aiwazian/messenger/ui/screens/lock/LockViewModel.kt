/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.ui.screens.settings.security.passcode.PasscodeViewModel
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LockUiState())
    val uiState = _uiState.asStateFlow()
    
    val fingerprintEnabled = appLockManager.fingerprintEnabled

    init {
        viewModelScope.launch {
            appLockManager.blockedUntil.collectLatest { blockedUntil ->
                _uiState.update { it.copy(blockedUntil = blockedUntil) }
                if (blockedUntil > System.currentTimeMillis()) {
                    startTimer(blockedUntil)
                }
            }
        }
    }

    private fun startTimer(blockedUntil: Long) {
        viewModelScope.launch {
            while (System.currentTimeMillis() < blockedUntil) {
                val remaining = ((blockedUntil - System.currentTimeMillis()) / 1000).toInt()
                _uiState.update { it.copy(remainingSeconds = remaining) }
                delay(1000)
            }
            _uiState.update { it.copy(blockedUntil = 0L, remainingSeconds = 0) }
            appLockManager.resetFailedAttempts()
        }
    }
    
    fun onPasscodeChanged(newPasscode: String) {
        vibrationManager.vibrate(VibrationPattern.TactileResponse)
        
        if (_uiState.value.blockedUntil > System.currentTimeMillis()) return

        if (newPasscode.length <= PasscodeViewModel.MAX_LENGTH_PASSCODE) {
            _uiState.update { it.copy(passcode = newPasscode) }
        }
        
        if (_uiState.value.passcode.length == PasscodeViewModel.MAX_LENGTH_PASSCODE) {
            checkPasscode()
        }
    }
    
    fun onFingerprintSuccess() {
        viewModelScope.launch {
            appLockManager.unlock()
        }
    }
    
    private fun clearPasscode() {
        _uiState.update { it.copy(passcode = "") }
    }
    
    private fun checkPasscode() {
        val currentPasscode = _uiState.value.passcode
        val isCorrect = appLockManager.checkPasscode(currentPasscode)
        
        if (isCorrect) {
            clearPasscode()
            viewModelScope.launch {
                appLockManager.unlock()
            }
        } else {
            vibrationManager.vibrate(VibrationPattern.Error)
            clearPasscode()
            viewModelScope.launch {
                appLockManager.incrementFailedAttempts()
            }
        }
    }
}
