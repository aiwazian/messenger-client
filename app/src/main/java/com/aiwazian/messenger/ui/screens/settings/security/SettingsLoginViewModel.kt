/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.ChangeLoginRequest
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
class SettingsLoginViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _newLogin = MutableStateFlow("")
    val newLogin = _newLogin.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<SettingsLoginSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun onInputNewLogin(newLogin: String) {
        val filtered = newLogin.filter { !it.isWhitespace() }.take(64)
        _newLogin.update { filtered }
        _errorMessage.update { null }
    }
    
    fun onChangeLogin() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            if (!checkValidLogin()) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _isLoading.update { true }
            
            try {
                val request = ChangeLoginRequest(_newLogin.value)
                val result = authRepository.changeLogin(request)
                if (result.isSuccess) {
                    _sideEffect.emit(SettingsLoginSideEffect.NavigateBack)
                } else {
                    _errorMessage.update { "Логин уже занят" }
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e("SettingsLoginViewModel", e.message.toString())
                _errorMessage.update { "Ошибка при смене логина" }
                vibrationManager.vibrate(VibrationPattern.Error)
            } finally {
                _isLoading.update { false }
            }
        }
    }
    
    private fun checkValidLogin(): Boolean {
        if (_newLogin.value.isEmpty()) {
            _errorMessage.update { "Введите логин" }
            return false
        }
        
        if (_newLogin.value.length < 5) {
            _errorMessage.update { "Минимум 5 символов" }
            return false
        }
        
        if (!RegexPatterns.LOGIN.matches(_newLogin.value)) {
            _errorMessage.update { "Недопустимые символы" }
            return false
        }
        
        _errorMessage.update { null }
        return true
    }
}
