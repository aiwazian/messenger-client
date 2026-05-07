/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.SignInRequest
import com.aiwazian.messenger.extensions.isNetworkError
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
class PasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<PasswordUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun changePassword(newPassword: String) {
        val password = newPassword.filter { it.toString().matches(RegexPatterns.PASSWORD) }
        _uiState.update { it.copy(password = password, errorText = null) }
    }
    
    fun setLogin(login: String) {
        _uiState.update { it.copy(login = login) }
    }
    
    fun signIn() {
        if (_uiState.value.isLoading) return
        
        val login = _uiState.value.login
        val password = _uiState.value.password

        if (password.isBlank()) {
            _uiState.update { it.copy(errorText = UiText.StringResource(R.string.enter_password)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        if (password.length < 5) {
            _uiState.update { it.copy(errorText = UiText.StringResource(R.string.min_length_5_characters)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        val deviceModel = deviceInfoProvider.getDeviceModel()
        val osVersion = deviceInfoProvider.getOsVersion()
        val osName = deviceInfoProvider.getOsName()
        
        val requestBody = SignInRequest(
            login,
            password,
            deviceModel,
            osVersion,
            osName
        )
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.signIn(requestBody).onSuccess { data ->
                SessionManager.setAuthorized(true)
                SessionManager.saveSession(data.userId, data.token, data.createdAt)
                _uiEffect.emit(PasswordUiEffect.NavigateToMainActivity)
            }.onFailure {
                if (it.isNetworkError()) {
                    _uiEffect.emit(PasswordUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_connect)))
                } else {
                    _uiEffect.emit(PasswordUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
                }
                vibrationManager.vibrate(VibrationPattern.Error)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
