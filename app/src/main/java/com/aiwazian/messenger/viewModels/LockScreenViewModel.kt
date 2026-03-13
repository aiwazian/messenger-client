package com.aiwazian.messenger.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.services.AppLockService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockScreenViewModel @Inject constructor(private val appLockService: AppLockService) :
    ViewModel() {
    
    var onWrongPasscode: () -> Unit = { }
    
    var passcode by mutableStateOf("")
        private set
    
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
        val isCorrect = appLockService.checkPasscode(passcode)
        
        if (isCorrect) {
            clearPasscode()
            viewModelScope.launch {
                appLockService.unlock()
            }
        } else {
            onWrongPasscode()
            clearPasscode()
        }
    }
}