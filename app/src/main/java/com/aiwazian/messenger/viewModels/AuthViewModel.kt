package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.data.AuthRequest
import com.aiwazian.messenger.data.RegisterRequest
import com.aiwazian.messenger.services.AuthService
import com.aiwazian.messenger.services.DeviceHelper
import com.aiwazian.messenger.services.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val deviceHelper: DeviceHelper
) : ViewModel() {
    
    private val _login = MutableStateFlow("")
    val login = _login.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    
    private val _loginFieldError = MutableStateFlow<String?>(null)
    val loginFieldError = _loginFieldError.asStateFlow()
    
    private val _passwordFieldError = MutableStateFlow<String?>(null)
    val passwordFieldError = _passwordFieldError.asStateFlow()
    
    private val _isUserFound = MutableStateFlow(false)
    val isUserFound = _isUserFound.asStateFlow()
    
    fun onLoginChanged(newLogin: String) {
        _login.update { newLogin.trim() }
        clearError()
    }
    
    fun onPasswordChanged(newPassword: String) {
        _password.update { newPassword.trim() }
        clearError()
    }
    
    suspend fun findUserByLogin(): Boolean? {
        try {
            val isFind = authService.findUserByLogin(login = _login.value)
            
            _isUserFound.update { isFind }
            
            return isFind
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при поиске пользователя с логином ${_login.value}",
                e
            )
            
            return null
        }
    }
    
    suspend fun onLoginClicked(): Boolean {
        try {
            val deviceName = deviceHelper.getDeviceName()
            
            val requestBody = AuthRequest(
                _login.value,
                _password.value,
                deviceName
            )
            
            val token = authService.login(requestBody)
            
            if (token == null) {
                return false
            }
            
            TokenManager.saveToken(token)
            TokenManager.setAuthorized(true)
            
            return true
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при авторизации",
                e
            )
            
            return false
        }
    }
    
    suspend fun onRegisterClicked(): Boolean {
        try {
            val requestBody = RegisterRequest(
                _login.value,
                _password.value
            )
            
            return authService.register(requestBody)
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при регистрации",
                e
            )
            
            return false
        }
    }
    
    fun checkValidLogin(): Boolean {
        if (_login.value.isBlank()) {
            _loginFieldError.update { "Введите логин" }
            return false
        }
        
        if (_login.value.trim().length < 5) {
            _loginFieldError.update { "Минимум 5 символов" }
            return false
        }
        
        _loginFieldError.update { null }
        
        return true
    }
    
    fun checkValidPassword(): Boolean {
        if (_password.value.isBlank()) {
            _passwordFieldError.update { "Введите пароль" }
            return false
        }
        
        if (_password.value.trim().length < 5) {
            _passwordFieldError.update { "Минимум 5 символов" }
            return false
        }
        
        _passwordFieldError.update { null }
        
        return true
    }
    
    private fun clearError() {
        _loginFieldError.update { null }
        _passwordFieldError.update { null }
    }
}
