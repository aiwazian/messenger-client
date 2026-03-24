/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.passcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    companion object {
        const val MAX_LENGTH_PASSCODE = 4
    }
    
    private val _uiState = MutableStateFlow(PasscodeUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PasscodeUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    val disablePasscodeDialog = DialogController()

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun onPasscodeChanged(newPasscode: String) {
        if (newPasscode.length <= MAX_LENGTH_PASSCODE) {
            _uiState.update { it.copy(passcode = newPasscode) }
        }
        
        if (_uiState.value.passcode.length == MAX_LENGTH_PASSCODE) {
            setPasscode()
        }
    }
    
    fun disablePasscode() {
        viewModelScope.launch {
            appLockManager.disablePasscode()
            _uiEffect.emit(PasscodeUiEffect.NavigateBack)
        }
    }
    
    private fun setPasscode() {
        viewModelScope.launch {
            appLockManager.changePasscode(_uiState.value.passcode)
            _uiEffect.emit(PasscodeUiEffect.NavigateBack)
        }
    }
}


