/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.AppLockManager
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsSecurityViewModel @Inject constructor(
    appLockManager: AppLockManager,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<SettingsSecuritySideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    init {
        viewModelScope.launch {
            sessionRepository.getDeviceCount().onSuccess { count ->
                _uiState.update { it.copy(deviceCount = count) }
            }
        }
        
        viewModelScope.launch {
            appLockManager.hasPasscode.collectLatest { passcode ->
                _uiState.update { it.copy(passcodeEnabled = passcode) }
            }
        }
    }
    
    fun onCloudPasswordClick() {
        viewModelScope.launch {
            val createdAt = authRepository.getCurrentAccountCreatedAt()
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMs = 24 * 60 * 60 * 1000L
            
            if (currentTime - createdAt < twentyFourHoursInMs) {
                _sideEffect.emit(
                    SettingsSecuritySideEffect.ShowSnackbar(
                        UiText.StringResource(R.string.change_password_session_age)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            } else {
                _sideEffect.emit(SettingsSecuritySideEffect.NavigateToCloudPassword)
            }
        }
    }
    
    fun onLoginClick() {
        viewModelScope.launch {
            val createdAt = authRepository.getCurrentAccountCreatedAt()
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMs = 24 * 60 * 60 * 1000L
            
            if (currentTime - createdAt < twentyFourHoursInMs) {
                _sideEffect.emit(
                    SettingsSecuritySideEffect.ShowSnackbar(
                        UiText.StringResource(R.string.change_login_session_age)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            } else {
                _sideEffect.emit(SettingsSecuritySideEffect.NavigateToLogin)
            }
        }
    }
    
    fun showBottomSheet() {
        _uiState.update { it.copy(showPasscodeBottomSheet = true) }
    }
    
    fun hideBottomSheet() {
        _uiState.update { it.copy(showPasscodeBottomSheet = false) }
    }
}
