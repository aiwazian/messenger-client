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
import com.aiwazian.messenger.ui.components.CodeInputStatus
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
class PasswordResetCodeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetCodeUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<PasswordResetCodeUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun setLogin(login: String) {
        _uiState.update { it.copy(login = login) }
    }
    
    fun onCodeChanged(code: String) {
        if (code.length <= 6) {
            _uiState.update { it.copy(code = code, errorText = null) }
            if (code.length == 6) {
                verifyCode(code)
            }
        }
    }
    
    fun onCodeInputStatusShown(status: CodeInputStatus) {
        _uiState.update { it.copy(codeStatus = CodeInputStatus.Default) }
    }
    
    private fun verifyCode(code: String) {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.verifyResetCode(_uiState.value.login, code).onSuccess { isValid ->
                if (isValid) {
                    _uiState.update { it.copy(codeStatus = CodeInputStatus.Success) }
                    _uiEffect.emit(
                        PasswordResetCodeUiEffect.NavigateToResetPassword(
                            login = _uiState.value.login,
                            code = code
                        )
                    )
                } else {
                    _uiState.update { it.copy(codeStatus = CodeInputStatus.Error, code = "") }
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            }.onError { error ->
                val message = when (error) {
                    ApiResult.Error.NoInternet,
                    ApiResult.Error.Timeout -> UiText.StringResource(R.string.failed_to_connect)
                    
                    ApiResult.Error.TooManyRequests -> UiText.StringResource(R.string.too_many_requests)
                    else -> UiText.StringResource(R.string.unexpected_error)
                }
                _uiEffect.emit(PasswordResetCodeUiEffect.ShowSnackbar(message))
                _uiState.update { it.copy(codeStatus = CodeInputStatus.Error, code = "") }
                vibrationManager.vibrate(VibrationPattern.Error)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
