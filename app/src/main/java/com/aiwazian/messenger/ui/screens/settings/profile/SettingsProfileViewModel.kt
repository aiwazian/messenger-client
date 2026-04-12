/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<SettingsProfileSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun onChangeFirstName(newName: String) {
        _uiState.update { it.copy(user = it.user.copy(firstName = newName)) }
    }

    fun onChangeLastName(newName: String) {
        _uiState.update { it.copy(user = it.user.copy(lastName = newName)) }
    }

    fun onChangeBio(newBio: String) {
        _uiState.update { it.copy(user = it.user.copy(bio = newBio)) }
    }

    fun onChangeDateOfBirth(newDate: Long?) {
        _uiState.update { it.copy(user = it.user.copy(dateOfBirth = newDate), showDatePicker = false) }
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun onSaveAndBack() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            userRepository.updateProfile(_uiState.value.user)
            _sideEffect.send(SettingsProfileSideEffect.NavigateBack)
        }
    }
}
