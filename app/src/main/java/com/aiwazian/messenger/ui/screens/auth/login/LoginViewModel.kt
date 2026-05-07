/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.RegexPatterns
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<LoginUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun changeLogin(newLogin: String) {
        val login = newLogin.filter { it.toString().matches(RegexPatterns.LOGIN) }
        _uiState.update { it.copy(login = login, errorText = null) }
    }
    
    fun checkLogin() {
        if (_uiState.value.isLoading) return
        
        val login = _uiState.value.login
        if (login.isBlank()) {
            _uiState.update { it.copy(errorText = UiText.StringResource(R.string.enter_login)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        if (login.length < 5) {
            _uiState.update { it.copy(errorText = UiText.StringResource(R.string.min_length_5_characters)) }
            vibrationManager.vibrate(VibrationPattern.Error)
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.checkLoginAvailable(login).onSuccess { find ->
                if (find) {
                    _uiState.update { it.copy(showNotFoundDialog = true) }
                } else {
                    _uiState.update { it.copy(showFoundDialog = true) }
                }
            }.onFailure {
                if (it.isNetworkError()) {
                    _uiEffect.emit(LoginUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_connect)))
                } else {
                    _uiEffect.emit(LoginUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
                }
                vibrationManager.vibrate(VibrationPattern.Error)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun hideFoundDialog() {
        _uiState.update { it.copy(showFoundDialog = false) }
    }
    
    fun hideNotFoundDialog() {
        _uiState.update { it.copy(showNotFoundDialog = false) }
    }
}
