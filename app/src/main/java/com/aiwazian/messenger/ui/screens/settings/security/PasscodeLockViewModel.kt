/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    companion object {
        const val MAX_LENGTH_PASSCODE = 4
    }
    
    private val _passcode = MutableStateFlow("")
    val passcode = _passcode.asStateFlow()
    
    val disablePasscodeDialog = DialogController()
    
    var onSaveNewPasscode: () -> Unit = { }

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun onPasscodeChanged(newPasscode: String) {
        if (newPasscode.length <= MAX_LENGTH_PASSCODE) {
            _passcode.update { newPasscode }
        }
        
        if (_passcode.value.length == MAX_LENGTH_PASSCODE) {
            setPasscode()
        }
    }
    
    suspend fun disablePasscode() {
        appLockManager.disablePasscode()
    }
    
    private fun setPasscode() {
        viewModelScope.launch {
            appLockManager.changePasscode(_passcode.value)
            onSaveNewPasscode()
        }
    }
}


