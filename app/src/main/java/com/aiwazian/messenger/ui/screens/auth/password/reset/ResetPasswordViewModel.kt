/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.network.ApiResult
import com.aiwazian.messenger.network.onError
import com.aiwazian.messenger.network.onSuccess
import com.aiwazian.messenger.repository.AuthRepository
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
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ResetPasswordUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun setLogin(login: String) {
        _uiState.update { it.copy(login = login) }
    }
    
    fun setCode(code: String) {
        _uiState.update { it.copy(code = code) }
    }
    
    fun onInputNewPassword(newPassword: String) {
        val filtered = newPassword.filter { it.toString().matches(RegexPatterns.PASSWORD) }
        _uiState.update { it.copy(newPassword = filtered, errorText = null) }
    }
    
    fun resetPassword() {
        if (_uiState.value.isLoading) return
        
        val password = _uiState.value.newPassword
        
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
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.resetPassword(_uiState.value.login, _uiState.value.code, password)
                .onSuccess { data ->
                    SessionManager.setAuthorized(true)
                    SessionManager.saveSession(data.userId, data.token, data.createdAt)
                    _uiEffect.emit(ResetPasswordUiEffect.NavigateToMainActivity)
                }
                .onError { error ->
                    when (error) {
                        ApiResult.Error.NoInternet,
                        ApiResult.Error.Timeout -> {
                            _uiEffect.emit(
                                ResetPasswordUiEffect.ShowSnackbar(
                                    UiText.StringResource(
                                        R.string.failed_to_connect
                                    )
                                )
                            )
                        }
                        
                        ApiResult.Error.Unauthorized -> {
                            _uiState.update { it.copy(errorText = UiText.StringResource(R.string.invalid_code)) }
                        }
                        
                        ApiResult.Error.TooManyRequests -> {
                            _uiEffect.emit(
                                ResetPasswordUiEffect.ShowSnackbar(
                                    UiText.StringResource(
                                        R.string.too_many_requests
                                    )
                                )
                            )
                        }
                        
                        else -> {
                            _uiEffect.emit(
                                ResetPasswordUiEffect.ShowSnackbar(
                                    UiText.StringResource(
                                        R.string.unexpected_error
                                    )
                                )
                            )
                        }
                    }
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
