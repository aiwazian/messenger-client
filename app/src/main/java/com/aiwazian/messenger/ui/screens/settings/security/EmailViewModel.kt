/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.RegexPatterns
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
class EmailViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<EmailSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun onInputEmail(email: String) {
        val filtered = email.filter { !it.isWhitespace() }.take(254)
        _email.update { filtered }
        _errorMessage.update { null }
    }
    
    fun onSendEmail() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            if (!checkValidEmail()) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _isLoading.update { true }
            
            try {
                val result = authRepository.setEmail(_email.value)
                if (result.isSuccess) {
                    _sideEffect.emit(EmailSideEffect.NavigateToVerify)
                } else {
                    _errorMessage.update { "Ошибка при отправке почты" }
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e("EmailViewModel", e.message.toString())
                _errorMessage.update { "Ошибка при отправке почты" }
                vibrationManager.vibrate(VibrationPattern.Error)
            } finally {
                _isLoading.update { false }
            }
        }
    }
    
    private fun checkValidEmail(): Boolean {
        if (_email.value.isEmpty()) {
            _errorMessage.update { "Введите электронный адрес" }
            return false
        }
        
        if (!RegexPatterns.EMAIL.matches(_email.value)) {
            _errorMessage.update { "Некорректный формат электронного адреса" }
            return false
        }
        
        _errorMessage.update { null }
        return true
    }
}
