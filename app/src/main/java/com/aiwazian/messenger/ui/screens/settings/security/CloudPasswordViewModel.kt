/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChangePasswordRequest
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
class CloudPasswordViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<CloudPasswordSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun onInputNewPassword(newPassword: String) {
        val filtered = newPassword.filter { !it.isWhitespace() }.take(64)
        _newPassword.update { filtered }
        _errorMessage.update { null }
    }
    
    fun onChangePassword() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            if (!checkValidPassword()) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _isLoading.update { true }
            
            try {
                val requestBody = ChangePasswordRequest(_newPassword.value)
                val result = authRepository.changePassword(requestBody)
                if (result.isSuccess) {
                    _sideEffect.emit(CloudPasswordSideEffect.NavigateBack)
                }
            } catch (e: Exception) {
                Log.e("CloudPasswordViewModel", e.message.toString())
            } finally {
                _isLoading.update { false }
            }
        }
    }
    
    private fun checkValidPassword(): Boolean {
        if (_newPassword.value.isEmpty()) {
            _errorMessage.update { UiText.StringResource(R.string.enter_password) }
            return false
        }

        if (_newPassword.value.length < 5) {
            _errorMessage.update { UiText.StringResource(R.string.min_length_5_characters) }
            return false
        }
        
        if (!RegexPatterns.PASSWORD.matches(_newPassword.value)) {
            _errorMessage.update { UiText.StringResource(R.string.invalid_characters) }
            return false
        }

        _errorMessage.update { null }
        return true
    }
}
