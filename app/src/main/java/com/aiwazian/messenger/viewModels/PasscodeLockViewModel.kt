package com.aiwazian.messenger.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.services.AppLockService
import com.aiwazian.messenger.services.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeLockViewModel @Inject constructor(private val appLockService: AppLockService) :
    ViewModel() {
    
    companion object {
        const val MAX_LENGTH_PASSCODE = 4
    }
    
    private val _passcode = MutableStateFlow("")
    val passcode = _passcode.asStateFlow()
    
    val disablePasscodeDialog = DialogController()
    
    var onSaveNewPasscode: () -> Unit = { }
    
    fun onPasscodeChanged(newPasscode: String) {
        if (newPasscode.length <= MAX_LENGTH_PASSCODE) {
            _passcode.update { newPasscode }
        }
        
        if (_passcode.value.length == MAX_LENGTH_PASSCODE) {
            setPasscode()
        }
    }
    
    suspend fun disablePasscode() {
        appLockService.disablePasscode()
    }
    
    private fun setPasscode() {
        viewModelScope.launch {
            appLockService.changePasscode(_passcode.value)
            onSaveNewPasscode()
        }
    }
}