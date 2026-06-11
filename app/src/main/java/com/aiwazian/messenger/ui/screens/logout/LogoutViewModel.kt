/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.AuthRepository
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
class LogoutViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
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
    
    fun logout() {
        viewModelScope.launch {
            val accounts = authRepository.getAllAccounts()
            val currentAccount = accounts.find { it.isCurrent }
            val otherAccounts =
                accounts.filter { it.userId != currentAccount?.userId && it.token.isNotEmpty() }
            
            authRepository.logout().onFailure { e ->
                Log.e(
                    "AuthManager",
                    "Ошибка при выходе: ${e.message}"
                )
            }
            
            if (otherAccounts.isNotEmpty()) {
                SessionManager.switchAccount(otherAccounts.first().userId)
                _sideEffect.emit(LogoutSideEffect.SwitchToNextAccount)
            } else {
                _sideEffect.emit(LogoutSideEffect.NoAccountsLeft)
            }
        }
    }
}
