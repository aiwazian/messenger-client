/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.ui.components.CodeInputStatus
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
import kotlin.time.Duration.Companion.seconds

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
                delay(1.seconds)
            }
            _uiState.update { it.copy(blockedUntil = 0L, remainingSeconds = 0) }
            appLockManager.resetFailedAttempts()
        }
    }
    
    fun onPasscodeChanged(newPasscode: String) {
        if (_uiState.value.status != CodeInputStatus.Default) return
        if (_uiState.value.blockedUntil > System.currentTimeMillis()) return
        
        vibrationManager.vibrate(VibrationPattern.TactileResponse)
        
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
    
    fun onStatusShown(status: CodeInputStatus) {
        when (status) {
            CodeInputStatus.Success -> {
                clearPasscode()
                _uiState.update { it.copy(status = CodeInputStatus.Default) }
                viewModelScope.launch { appLockManager.unlock() }
            }
            
            CodeInputStatus.Error -> {
                clearPasscode()
                _uiState.update { it.copy(status = CodeInputStatus.Default) }
                viewModelScope.launch { appLockManager.incrementFailedAttempts() }
            }
            
            CodeInputStatus.Default -> {}
        }
    }
    
    private fun clearPasscode() {
        _uiState.update { it.copy(passcode = "") }
    }
    
    private fun checkPasscode() {
        val currentPasscode = _uiState.value.passcode
        val isCorrect = appLockManager.checkPasscode(currentPasscode)
        
        if (isCorrect) {
            _uiState.update { it.copy(status = CodeInputStatus.Success) }
        } else {
            vibrationManager.vibrate(VibrationPattern.Error)
            _uiState.update { it.copy(status = CodeInputStatus.Error) }
        }
    }
}
