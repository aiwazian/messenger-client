/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.AuthRepository
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
class EmailConfigViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _email = MutableStateFlow<String?>(null)
    val email = _email.asStateFlow()
    
    private val _showDisableDialog = MutableStateFlow(false)
    val showDisableDialog = _showDisableDialog.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<EmailConfigSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    init {
        loadEmail()
    }
    
    private fun loadEmail() {
        viewModelScope.launch {
            authRepository.getEmail().onSuccess { response ->
                _email.update { response.email }
            }
        }
    }
    
    fun onChangeEmailClick() {
        viewModelScope.launch {
            _sideEffect.emit(EmailConfigSideEffect.NavigateToChangeEmail)
        }
    }
    
    fun onDisableEmailClick() {
        _showDisableDialog.update { true }
    }
    
    fun hideDisableDialog() {
        _showDisableDialog.update { false }
    }
    
    fun confirmDisableEmail() {
        viewModelScope.launch {
            try {
                val result = authRepository.disableEmail()
                if (result.isSuccess) {
                    _showDisableDialog.update { false }
                    _sideEffect.emit(EmailConfigSideEffect.NavigateBack)
                } else {
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e("EmailConfigViewModel", e.message.toString())
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
