package com.aiwazian.messenger.services

import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object UserManager {
    
    private val _userInfo = MutableStateFlow(UserInfo())
    val user = _userInfo.asStateFlow()
    
    fun updateUserInfo(updatedUserInfo: UserInfo) {
        val newUserInfo = _userInfo.value.copy(
            firstName = updatedUserInfo.firstName,
            lastName = updatedUserInfo.lastName,
            bio = updatedUserInfo.bio,
            username = updatedUserInfo.username,
            dateOfBirth = updatedUserInfo.dateOfBirth,
        )
        _userInfo.update { newUserInfo }
    }
    
    suspend fun loadUserData(userRepository: UserRepository) {
        val user = userRepository.getMe()
        
        if (user != null) {
            _userInfo.update { user }
        }
    }
}