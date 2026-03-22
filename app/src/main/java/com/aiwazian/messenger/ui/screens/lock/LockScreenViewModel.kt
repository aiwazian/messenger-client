/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.ui.screens.settings.security.PasscodeLockViewModel
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    var passcode by mutableStateOf("")
        private set

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun onPasscodeChanged(newPasscode: String) {
        if (newPasscode.length <= PasscodeLockViewModel.MAX_LENGTH_PASSCODE) {
            passcode = newPasscode
        }
        
        if (passcode.length == PasscodeLockViewModel.MAX_LENGTH_PASSCODE) {
            checkPasscode()
        }
    }
    
    private fun clearPasscode() {
        passcode = ""
    }
    
    private fun checkPasscode() {
        val isCorrect = appLockManager.checkPasscode(passcode)
        
        if (isCorrect) {
            clearPasscode()
            viewModelScope.launch {
                appLockManager.unlock()
            }
        } else {
            vibrate(VibrationPattern.Error)
            clearPasscode()
        }
    }
}


