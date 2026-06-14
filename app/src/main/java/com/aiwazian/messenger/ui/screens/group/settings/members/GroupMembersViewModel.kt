/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.UiText
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
class GroupMembersViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupMembersState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<GroupMembersSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private var _groupId: Long = -1L
    
    fun init(groupId: Long) {
        _groupId = groupId
        
        viewModelScope.launch {
            groupRepository.getMembers(_groupId).collectLatest { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
    }
    
    fun onKickClick(userId: Long) {
        _uiState.update { it.copy(selectedUserId = userId) }
        viewModelScope.launch {
            _sideEffect.emit(GroupMembersSideEffect.ShowKickConfirmation)
        }
    }
    
    fun onBlockClick(userId: Long) {
        _uiState.update { it.copy(selectedUserId = userId) }
        viewModelScope.launch {
            _sideEffect.emit(GroupMembersSideEffect.ShowBlockConfirmation)
        }
    }
    
    fun confirmKick() {
        val userId = _uiState.value.selectedUserId ?: return
        viewModelScope.launch {
            groupRepository.kickUser(_groupId, userId).onSuccess {
                _uiState.update { state ->
                    state.copy(members = state.members.filter { it.id != userId })
                }
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar(UiText.StringResource(R.string.user_kicked)))
            }.onFailure {
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
            }
            _uiState.update { it.copy(selectedUserId = null) }
        }
    }
    
    fun confirmBlock() {
        val userId = _uiState.value.selectedUserId ?: return
        viewModelScope.launch {
            groupRepository.banUser(_groupId, userId).onSuccess {
                _uiState.update { state ->
                    state.copy(members = state.members.filter { it.id != userId })
                }
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar(UiText.StringResource(R.string.user_blocked)))
            }.onFailure {
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
            }
            _uiState.update { it.copy(selectedUserId = null) }
        }
    }
}
