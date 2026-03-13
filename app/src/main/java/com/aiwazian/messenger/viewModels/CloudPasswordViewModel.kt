package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChangeCloudPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CloudPasswordViewModel : ViewModel() {
    
    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    fun onInputNewPassword(newPassword: String) {
        _newPassword.update { newPassword }
        _errorMessage.update { null }
    }
    
    fun checkValidPassword(): Boolean {
        if (_newPassword.value.isBlank()) {
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
        try {
            val requestBody = ChangeCloudPasswordRequest(_newPassword.value)
            val res = RetrofitInstance.api.changeCloudPassword(requestBody)
            
            if (!res.isSuccessful) {
                return false
            }
            
            return res.code() == 200
        } catch (e: Exception) {
            Log.e(
                "CloudPasswordViewModel",
                e.message.toString()
            )
            
            return false
        }
    }
    
    fun cleanData() {
        _newPassword.update { "" }
        _errorMessage.update { null }
    }
}