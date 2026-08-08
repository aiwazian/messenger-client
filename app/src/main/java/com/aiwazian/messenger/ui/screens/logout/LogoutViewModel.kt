/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.SessionEndResolution
import com.aiwazian.messenger.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(LogoutUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<LogoutSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun showLogoutDialog() {
        _uiState.update { it.copy(isLogoutDialogVisible = true) }
    }
    
    fun hideLogoutDialog() {
        _uiState.update { it.copy(isLogoutDialogVisible = false) }
    }
    
    /**
     * Выход касается только текущего аккаунта: если на устройстве есть другой
     * аккаунт, приложение переключается на него вместо экрана авторизации.
     */
    fun logout() {
        viewModelScope.launch {
            val resolution = SessionManager.endCurrentSessionAndResolve(revokeOnServer = true)
            
            val effect = when (resolution) {
                is SessionEndResolution.SwitchedToAccount -> LogoutSideEffect.SwitchToNextAccount
                is SessionEndResolution.NoAccountsLeft -> LogoutSideEffect.NoAccountsLeft
            }
            
            _sideEffect.emit(effect)
        }
    }
}
