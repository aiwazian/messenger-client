/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManager @Inject constructor(
    private val userRepository: UserRepository
) {
    
    private val _user = MutableStateFlow(User())
    val user = _user.asStateFlow()
    
    fun updateUserInfo(updatedUser: User) {
        _user.update {
            it.copy(
                firstName = updatedUser.firstName,
                lastName = updatedUser.lastName,
                bio = updatedUser.bio,
                username = updatedUser.username,
                dateOfBirth = updatedUser.dateOfBirth,
            )
        }
    }
    
    suspend fun loadUserData() {
        userRepository.getMe().collect { user ->
            _user.update { user }
        }
    }
}
