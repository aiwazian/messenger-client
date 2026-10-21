/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageReadersViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _avatars = MutableStateFlow<Map<Long, Uri?>>(emptyMap())
    val avatars = _avatars.asStateFlow()
    
    private val observedUserIds = mutableSetOf<Long>()
    
    fun onReadersRequested(userIds: List<Long>) {
        userIds.forEach { userId ->
            if (!observedUserIds.add(userId)) return@forEach
            
            viewModelScope.launch {
                userRepository.fetchById(userId)
            }
            
            viewModelScope.launch {
                userRepository.getByIdOrNull(userId).collect { user ->
                    val avatarUri = user?.avatars?.firstOrNull()?.uri
                    _avatars.update { avatars -> avatars + (userId to avatarUri) }
                }
            }
        }
    }
}
