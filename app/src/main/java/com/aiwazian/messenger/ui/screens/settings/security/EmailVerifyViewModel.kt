/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerifyViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _code = MutableStateFlow("")
    val code = _code.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<EmailVerifySideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun onInputCode(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _code.update { filtered }
        _errorMessage.update { null }
        
        if (filtered.length == 6) {
            onVerifyCode()
        }
    }
    
    private fun onVerifyCode() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.update { true }
            
            try {
                val result = authRepository.verifyEmail(_code.value)
                if (result.isSuccess) {
                    _sideEffect.emit(EmailVerifySideEffect.NavigateToConfig)
                } else {
                    _errorMessage.update { "Неверный код" }
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e("EmailVerifyViewModel", e.message.toString())
                _errorMessage.update { "Неверный код" }
                vibrationManager.vibrate(VibrationPattern.Error)
            } finally {
                _isLoading.update { false }
            }
        }
    }
}
