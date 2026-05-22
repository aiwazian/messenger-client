/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsUsernameViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UsernameUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<UsernameUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var checkJob: Job? = null
    
    fun initUsername(username: String?) {
        _uiState.update {
            it.copy(
                username = username.orEmpty(),
                originalName = username.orEmpty()
            )
        }
    }
    
    fun onChangeUsername(newUsername: String) {
        val filteredUsername =
            newUsername.filter { it.toString().matches(RegexPatterns.SET_USERNAME) }
        _uiState.update { it.copy(username = filteredUsername) }
        
        if (filteredUsername.isBlank()) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        if (filteredUsername.length < 5) {
            _uiState.update {
                it.copy(
                    isError = true,
                    canSave = false,
                    statusText = UiText.StringResource(R.string.min_length_5_characters)
                )
            }
            return
        }
        
        if (filteredUsername == _uiState.value.originalName) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        _uiState.update {
            it.copy(
                isError = false,
                canSave = false,
                statusText = UiText.DynamicString("Проверка")
            )
        }
        
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            delay(500)
            searchRepository.checkUsernameAvailable(filteredUsername).onSuccess { available ->
                if (available) {
                    _uiState.update {
                        it.copy(
                            canSave = true,
                            isError = false,
                            statusText = UiText.StringResource(R.string.username_available)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            canSave = false,
                            isError = true,
                            statusText = UiText.StringResource(R.string.username_taken)
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isError = true,
                        canSave = false,
                        statusText = UiText.StringResource(R.string.unexpected_error)
                    )
                }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            if (_uiState.value.username == _uiState.value.originalName) {
                _uiEffect.emit(UsernameUiEffect.NavigateBack)
                return@launch
            }
            
            userRepository.saveUsername(_uiState.value.username).onSuccess {
                _uiEffect.emit(UsernameUiEffect.NavigateBack)
            }.onFailure {
                if (it.isNetworkError()) {
                    _uiEffect.emit(UsernameUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_connect)))
                } else {
                    _uiEffect.emit(UsernameUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
                }
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
