package com.aiwazian.messenger.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.repository.UserRepository
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsProfileViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {
    
    private val _userInfo = MutableStateFlow(UserInfo())
    val user = _userInfo.asStateFlow()
    
    val dataOfBirthDialog = DialogController()
    
    init {
        viewModelScope.launch {
            UserManager.user.collectLatest { collect ->
                _userInfo.update { collect }
            }
        }
    }
    
    fun onChangeFirstName(newName: String) {
        _userInfo.update { it.copy(firstName = newName) }
    }
    
    fun onChangeLastName(newName: String) {
        _userInfo.update { it.copy(lastName = newName) }
    }
    
    fun onChangeBio(newBio: String) {
        _userInfo.update { it.copy(bio = newBio) }
    }
    
    fun onChangeDateOfBirth(newDate: Long?) {
        _userInfo.update { it.copy(dateOfBirth = newDate) }
    }
    
    suspend fun save() {
        UserManager.updateUserInfo(_userInfo.value)
        userRepository.updateProfile(_userInfo.value)
    }
}