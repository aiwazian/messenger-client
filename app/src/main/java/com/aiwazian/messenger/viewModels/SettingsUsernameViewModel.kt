package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.services.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsUsernameViewModel @Inject constructor(private val userService: UserService) :
    ViewModel() {
    
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    
    private val _canSave = MutableStateFlow(false)
    val canSave = _canSave.asStateFlow()
    
    var errorText by mutableStateOf<String?>(null)
        private set
    
    fun init() {
        viewModelScope.launch {
            UserManager.user.collectLatest { collect ->
                _username.update { collect.username.orEmpty() }
            }
        }
        
        updateErrorMessage(null)
    }
    
    fun onChangeUsername(newUsername: String) {
        val validUsername = newUsername.trim()
        
        _username.update { validUsername }
        
        if (validUsername.isEmpty()) {
            updateErrorMessage(null)
            _canSave.update { true }
            return
        }
        
        if (validUsername.isNotEmpty() && validUsername.length < 5) {
            updateErrorMessage("Минимальная длина 5 символов")
            _canSave.update { false }
            return
        }
        
        if (validUsername.length > 20) {
            updateErrorMessage("Максимальная длина 20 символов")
            _canSave.update { false }
            return
        }
        
        if (validUsername == UserManager.user.value.username) {
            updateErrorMessage(null)
            _canSave.update { true }
            return
        }
        
        updateErrorMessage("Проверка имени")
        
        viewModelScope.launch {
            try {
                val isBusy = userService.checkUsername(_username.value)
                
                updateErrorMessage(
                    if (isBusy) {
                        "Имя пользователя занято"
                    } else {
                        "Имя пользователя свободно"
                    }
                )
                
                _canSave.update { !isBusy }
            } catch (e: Exception) {
                _canSave.update { false }
                Log.e("SettingsUsernameVM", e.toString())
            }
        }
    }
    
    suspend fun trySave(): Boolean {
        val username = _username.value.ifEmpty { null }
        
        if (username == UserManager.user.value.username) {
            return true
        }
        
        val isSaved = userService.saveUsername(username ?: "")
        
        if (isSaved) {
            val updatedUser = UserManager.user.value.copy(username = username)
            
            UserManager.updateUserInfo(updatedUser)
        }
        
        return isSaved
    }
    
    private fun updateErrorMessage(newMessage: String?) {
        errorText = newMessage
    }
}