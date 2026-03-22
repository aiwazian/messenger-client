/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userManager: UserManager
) :
    ViewModel() {
    
    private val _user = MutableStateFlow(User())
    val user = _user.asStateFlow()
    
    val dataOfBirthDialog = DialogController()
    
    init {
        viewModelScope.launch {
            userManager.user.collectLatest { collect ->
                _user.update { collect }
            }
        }
    }
    
    fun onChangeFirstName(newName: String) {
        _user.update { it.copy(firstName = newName) }
    }
    
    fun onChangeLastName(newName: String) {
        _user.update { it.copy(lastName = newName) }
    }
    
    fun onChangeBio(newBio: String) {
        _user.update { it.copy(bio = newBio) }
    }
    
    fun onChangeDateOfBirth(newDate: Long?) {
        _user.update { it.copy(dateOfBirth = newDate) }
    }
    
    suspend fun save() {
        userManager.updateUserInfo(_user.value)
        userRepository.updateProfile(_user.value)
    }
}


