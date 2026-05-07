/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.blockedUsers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    
    private var selectedUser: User? = null
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            groupRepository.getBannedUsers(groupId).onSuccess { users ->
                _uiState.update { it.copy(blockedUsers = users) }
            }
        }
    }
    
    fun onUnblockClick(user: User) {
        selectedUser = user
        _uiState.update { it.copy(showUnblockDialog = true) }
    }
    
    fun confirmUnblock() {
        val user = selectedUser ?: return
        viewModelScope.launch {
            groupRepository.unban(_uiState.value.groupId, user.id).onSuccess {
                _uiState.update { state ->
                    state.copy(blockedUsers = state.blockedUsers.filter { it.id != user.id })
                }
                _sideEffect.emit(GroupBlockedUsersSideEffect.ShowSnackbar(UiText.StringResource(R.string.user_unblocked)))
            }.onFailure {
                _sideEffect.emit(GroupBlockedUsersSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_unblock_user)))
            }
            selectedUser = null
        }
    }
    
    fun hideUnblockDialog() {
        _uiState.update { it.copy(showUnblockDialog = false) }
    }
}
