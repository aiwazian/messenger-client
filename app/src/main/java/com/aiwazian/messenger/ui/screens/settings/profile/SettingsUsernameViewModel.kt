/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsUsernameViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave = _canSave.asStateFlow()

    var errorText by mutableStateOf<String?>(null)
        private set

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }

    fun init() {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _username.update { user.username.orEmpty() }
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

        updateErrorMessage("Проверка имени")

        viewModelScope.launch {
            try {
                val isAvailable = searchRepository.checkUsernameAvailable(_username.value)

                updateErrorMessage(
                    if (!isAvailable) {
                        "Имя пользователя занято"
                    } else {
                        "Имя пользователя свободно"
                    }
                )

                _canSave.update { isAvailable }
            } catch (e: Exception) {
                _canSave.update { false }
                Log.e("SettingsUsernameVM", e.toString())
            }
        }
    }

    suspend fun trySave(): Boolean {
        val username = _username.value.ifEmpty { null }

        val isSaved = userRepository.saveUsername(username ?: "")

        return isSaved
    }

    private fun updateErrorMessage(newMessage: String?) {
        errorText = newMessage
    }
}


