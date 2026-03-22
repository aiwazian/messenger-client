/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.DeviceInfoProvider
import com.aiwazian.messenger.utils.SessionManager
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
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiEffect = MutableSharedFlow<AuthUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val _login = MutableStateFlow("")
    val login = _login.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    
    private val _loginFieldError = MutableStateFlow<String?>(null)
    val loginFieldError = _loginFieldError.asStateFlow()
    
    private val _passwordFieldError = MutableStateFlow<String?>(null)
    val passwordFieldError = _passwordFieldError.asStateFlow()
    
    private val _isUserFound = MutableStateFlow(false)
    
    private val _isLoadingLogin = MutableStateFlow(false)
    val isLoadingLogin = _isLoadingLogin.asStateFlow()
    
    private val _isLoadingPassword = MutableStateFlow(false)
    val isLoadingPassword = _isLoadingPassword.asStateFlow()
    
    private val _checkError = MutableStateFlow<String?>(null)
    val checkError = _checkError.asStateFlow()
    
    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun onLoginChanged(newLogin: String) {
        _login.update { newLogin.trim() }
        clearError()
        _checkError.value = null
    }
    
    fun onPasswordChanged(newPassword: String) {
        _password.update { newPassword.trim() }
        clearError()
    }
    
    suspend fun findUserByLogin(): Boolean? {
        return try {
            val result = authRepository.checkLoginAvailable(_login.value)
            when (result) {
                true -> {
                    // Логин свободен - пользователь не найден
                    _isUserFound.update { false }
                    _checkError.value = null
                    false
                }
                
                false -> {
                    // Логин занят - пользователь найден
                    _isUserFound.update { true }
                    _checkError.value = null
                    true
                }
                
                null -> {
                    // Ошибка сети или сервера
                    _checkError.value = "Не удалось подключиться к серверу"
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при поиске пользователя с логином ${_login.value}",
                e
            )
            _checkError.value = "Ошибка сети"
            null
        }
    }
    
    suspend fun onLoginClicked(): Result<Unit> {
        return try {
            if (signIn()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Не удалось войти в аккаунт"))
            }
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при авторизации",
                e
            )
            Result.failure(e)
        }
    }
    
    suspend fun onRegisterClicked(): Result<String> {
        return try {
            if (signUp()) {
                signIn()
                Result.success("")
            } else {
                Result.failure(Exception("Ошибка регистрации"))
            }
        } catch (e: Exception) {
            Log.e(
                "AuthViewModel",
                "Ошибка при регистрации",
                e
            )
            Result.failure(e)
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
    
    fun onLoginNextClicked() {
        if (!checkValidLogin()) {
            vibrate(VibrationPattern.Error)
            return
        }
        
        _isLoadingLogin.update { true }
        
        viewModelScope.launch {
            val result = findUserByLogin()
            
            _isLoadingLogin.update { false }
            
            _uiEffect.emit(AuthUiEffect.ShowLoginDialog(result))
        }
    }
    
    fun hideLoginDialog() {
        viewModelScope.launch {
            _uiEffect.emit(AuthUiEffect.HideLoginDialog)
        }
    }
    
    fun navigateToPassword() {
        viewModelScope.launch {
            _uiEffect.emit(AuthUiEffect.NavigateToPassword)
        }
    }
    
    fun onPasswordNextClicked() {
        viewModelScope.launch {
            if (!checkValidPassword()) {
                vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _isLoadingPassword.update { true }
            
            if (_isUserFound.value) {
                val result = onLoginClicked()
                if (result.isSuccess) {
                    _uiEffect.emit(AuthUiEffect.NavigateToMain)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message
                        ?: "Не удалось войти в аккаунт"
                    _uiEffect.emit(
                        AuthUiEffect.ShowPasswordDialog(
                            "login",
                            errorMessage
                        )
                    )
                }
            } else {
                val result = onRegisterClicked()
                if (result.isSuccess) {
                    _uiEffect.emit(AuthUiEffect.NavigateToMain)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message
                        ?: "Не удалось создать пользователя"
                    _uiEffect.emit(
                        AuthUiEffect.ShowPasswordDialog(
                            "register",
                            errorMessage
                        )
                    )
                }
            }
            
            _isLoadingPassword.update { false }
        }
    }
    
    fun hidePasswordDialog() {
        viewModelScope.launch {
            _uiEffect.emit(AuthUiEffect.HidePasswordDialog)
        }
    }
    
    private suspend fun signIn(): Boolean {
        val deviceName = deviceInfoProvider.getDeviceName()
        val osVersion = deviceInfoProvider.getOsVersion()
        val osName = deviceInfoProvider.getOsName()
        
        val requestBody = SignInRequest(
            _login.value,
            _password.value,
            deviceName,
            osVersion,
            osName
        )
        
        val result = authRepository.signIn(requestBody)
        val data = result.getOrNull() ?: return false
        
        SessionManager.setAuthorized(true)
        
        SessionManager.saveSession(data.userId, data.token)
        
        return true
    }
    
    private suspend fun signUp(): Boolean {
        val requestBody = SignUpRequest(
            _login.value,
            _password.value
        )
        
        val result = authRepository.signUp(requestBody)
        return result.isSuccess
    }
}
