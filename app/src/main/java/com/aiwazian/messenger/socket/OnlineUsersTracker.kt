/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineUsersTracker @Inject constructor() {
    private val _onlineUsers = MutableStateFlow<Set<Long>>(emptySet())
    val onlineUsers: StateFlow<Set<Long>> = _onlineUsers.asStateFlow()
    
    fun setOnline(userId: Long) {
        _onlineUsers.update { it + userId }
    }
    
    fun setOffline(userId: Long) {
        _onlineUsers.update { it - userId }
    }
    
    fun replaceAll(userIds: List<Long>) {
        _onlineUsers.update { userIds.toSet() }
    }
    
    fun isOnline(userId: Long): Boolean = userId in _onlineUsers.value
}
