/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.SignUpRequest
import com.aiwazian.messenger.network.ApiResult
import com.aiwazian.messenger.network.onError
import com.aiwazian.messenger.network.onSuccess
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.DeviceInfoProvider
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.SessionManager
import com.aiwazian.messenger.utils.UiText
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<RegisterUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun changeFirstName(name: String) {
        _uiState.update { it.copy(firstName = name, firstNameFieldError = null) }
    }
    
    fun changeLastName(name: String) {
        _uiState.update { it.copy(lastName = name) }
    }
    
    fun changePassword(newPassword: String) {
        val password = newPassword.filter { it.toString().matches(RegexPatterns.PASSWORD) }
        _uiState.update { it.copy(password = password, passwordFieldError = null) }
    }
    
    fun setLogin(login: String) {
        _uiState.update { it.copy(login = login) }
    }
    
    fun changePrivacyCheck(check: Boolean) {
        _uiState.update {
            it.copy(
                checkedPrivacyTerms = check,
                isPrivacyError = false
            )
        }
    }
    
    fun signUp() {
        if (_uiState.value.isLoading) return
        
        val firstName = _uiState.value.firstName
        if (firstName.isBlank()) {
            _uiState.update { it.copy(firstNameFieldError = UiText.StringResource(R.string.enter_first_name)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        val password = _uiState.value.password
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordFieldError = UiText.StringResource(R.string.enter_password)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        if (password.length < 5) {
            _uiState.update { it.copy(passwordFieldError = UiText.StringResource(R.string.min_length_5_characters)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        if (!_uiState.value.checkedPrivacyTerms) {
            _uiState.update { it.copy(isPrivacyError = true) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        val login = _uiState.value.login
        val deviceModel = deviceInfoProvider.getDeviceModel()
        val osVersion = deviceInfoProvider.getOsVersion()
        val osName = deviceInfoProvider.getOsName()
        
        val requestBody = SignUpRequest(
            firstName,
            login,
            password,
            deviceModel,
            osVersion,
            osName
        )
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.signUp(requestBody).onSuccess { data ->
                SessionManager.setAuthorized(true)
                SessionManager.saveSession(data.userId, data.token, data.createdAt)
                _uiEffect.emit(RegisterUiEffect.NavigateToMainActivity)
            }.onError { error ->
                when (error) {
                    ApiResult.Error.NoInternet,
                    ApiResult.Error.Timeout -> {
                        _uiEffect.emit(RegisterUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_connect)))
                    }
                    
                    ApiResult.Error.TooManyRequests -> {
                        _uiEffect.emit(
                            RegisterUiEffect.ShowSnackbar(UiText.StringResource(R.string.too_many_requests))
                        )
                    }
                    
                    else -> {
                        _uiEffect.emit(
                            RegisterUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error))
                        )
                    }
                }
                
                vibrationManager.vibrate(VibrationPattern.Error)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
