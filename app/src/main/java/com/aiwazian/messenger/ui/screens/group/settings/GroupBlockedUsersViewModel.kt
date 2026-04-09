/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupBlockedUsersViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupBlockedUsersState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupBlockedUsersSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    private var _groupId: Long = -1L
    private var selectedUser: User? = null
    
    private var isInitialized = false

    fun init(groupId: Long) {
        if (isInitialized) return
        isInitialized = true
        _groupId = groupId
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupRepository.getBlackList(_groupId).collectLatest { users ->
                _uiState.update { it.copy(blockedUsers = users, isLoading = false) }
            }
        }
    }

    fun onUnblockClick(user: User) {
        selectedUser = user
        viewModelScope.launch {
            _sideEffect.emit(GroupBlockedUsersSideEffect.ShowUnblockConfirmation)
        }
    }

    fun confirmUnblock() {
        val user = selectedUser ?: return
        viewModelScope.launch {
            val result = groupRepository.unban(_groupId, user.id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(blockedUsers = state.blockedUsers.filter { it.id != user.id })
                }
                _sideEffect.emit(GroupBlockedUsersSideEffect.ShowSnackbar("Пользователь разблокирован"))
            } else {
                _sideEffect.emit(GroupBlockedUsersSideEffect.ShowSnackbar("Ошибка при разблокировке"))
            }
            selectedUser = null
        }
    }
}
