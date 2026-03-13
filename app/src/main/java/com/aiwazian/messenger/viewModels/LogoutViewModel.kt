package com.aiwazian.messenger.viewModels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.LoginActivity
import com.aiwazian.messenger.services.AuthService
import com.aiwazian.messenger.services.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(private val authService: AuthService) : ViewModel() {
    
    val logoutDialog = DialogController()
    
    suspend fun logout(context: Context) {
        try {
            authService.logout()
        } catch (e: Exception) {
            Log.e(
                "AuthManager",
                "Ошибка при выходе: ${e.message}"
            )
        }
        
        val intent = Intent(
            context,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        (context as Activity).finish()
    }
}