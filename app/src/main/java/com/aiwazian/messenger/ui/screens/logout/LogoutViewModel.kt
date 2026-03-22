/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.logout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.AuthActivity
import com.aiwazian.messenger.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LogoutUiState())
    val uiState = _uiState.asStateFlow()
    
    fun showLogoutDialog() {
        _uiState.update { it.copy(isLogoutDialogVisible = true) }
    }
    
    fun hideLogoutDialog() {
        _uiState.update { it.copy(isLogoutDialogVisible = false) }
    }
    
    suspend fun logout(context: Context) {
        try {
            authRepository.logout()
        } catch (e: Exception) {
            Log.e(
                "AuthManager",
                "Ошибка при выходе: ${e.message}"
            )
        }
        
        val intent = Intent(
            context,
            AuthActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        (context as Activity).finish()
    }
}
