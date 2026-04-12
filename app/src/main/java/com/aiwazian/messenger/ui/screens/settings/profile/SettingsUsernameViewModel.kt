/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsUsernameViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UsernameScreenUiState())
    val uiState = _uiState.asStateFlow()
    
    private var checkJob: Job? = null
    private val usernameRegex = Regex("^[a-zA-Z0-9_]*$")
    
    fun initUsername(initialUsername: String?) {
        if (initialUsername != null) {
            _uiState.update { it.copy(username = initialUsername) }
        }
    }
    
    fun onChangeUsername(newUsername: String) {
        val filteredUsername = newUsername.filter { it.toString().matches(usernameRegex) }
        
        _uiState.update { it.copy(username = filteredUsername, status = UsernameUiState.Idle) }
        
        if (filteredUsername.isEmpty()) {
            _uiState.update { it.copy(status = UsernameUiState.Available) }
            return
        }
        
        if (filteredUsername.length < 5) {
            _uiState.update { it.copy(status = UsernameUiState.Invalid("Минимальная длина 5 символов")) }
            return
        }
        
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _uiState.update { it.copy(status = UsernameUiState.Checking) }
            delay(500)
            try {
                val isAvailable = searchRepository.checkUsernameAvailable(filteredUsername)
                _uiState.update {
                    it.copy(
                        status = if (isAvailable) UsernameUiState.Available else UsernameUiState.Unavailable(
                            "Имя пользователя занято"
                        )
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(status = UsernameUiState.Invalid("Ошибка проверки")) }
            }
        }
    }
    
    suspend fun save(): Boolean {
        return userRepository.saveUsername(_uiState.value.username)
    }
}
