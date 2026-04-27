/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsSecurityViewModel @Inject constructor(
    appLockManager: AppLockManager,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            try {
                val count = sessionRepository.getDeviceCount()
                _uiState.update { it.copy(deviceCount = count) }
            } catch (e: Exception) {
                Log.e("SettingsSecurityViewModel", "Error init", e)
            }
        }
        viewModelScope.launch {
            appLockManager.hasPasscode.collectLatest { passcode ->
                _uiState.update { it.copy(passcodeEnabled = passcode) }
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
