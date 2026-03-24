/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.domain.ChangePasswordRequest
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CloudPasswordViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }

    fun onInputNewPassword(newPassword: String) {
        val filtered = newPassword.filter { char ->
            (char in 'a'..'z') || (char in 'A'..'Z') || (char in '0'..'9') ||
                    "_!@#$%^&*()-+=[]{}|;:',.<>?/`\"~".contains(char)
        }.take(32)
        _newPassword.update { filtered }
        _errorMessage.update { null }
    }

    fun checkValidPassword(): Boolean {
        if (_newPassword.value.isEmpty()) {
            _errorMessage.update { "Введите пароль" }
            return false
        }

        if (_newPassword.value.length < 5) {
            _errorMessage.update { "Минимум 5 символов" }
            return false
        }

        _errorMessage.update { null }
        return true
    }

    suspend fun changePassword(): Boolean {
        return try {
            val requestBody = ChangePasswordRequest(_newPassword.value)
            val result = authRepository.changePassword(requestBody)
            result.isSuccess
        } catch (e: Exception) {
            Log.e("CloudPasswordViewModel", e.message.toString())
            false
        }
    }
}


