/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

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
class GroupMembersViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupMembersState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupMembersSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    private var _groupId: Long = -1L
    private var selectedUser: User? = null
    
    private var isInitialized = false

    fun init(groupId: Long) {
        if (isInitialized) return
        isInitialized = true
        _groupId = groupId
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupRepository.getMembers(_groupId).collectLatest { members ->
                _uiState.update { it.copy(members = members, isLoading = false) }
            }
        }
    }

    fun onKickClick(user: User) {
        selectedUser = user
        viewModelScope.launch {
            _sideEffect.emit(GroupMembersSideEffect.ShowKickConfirmation)
        }
    }

    fun onBlockClick(user: User) {
        selectedUser = user
        viewModelScope.launch {
            _sideEffect.emit(GroupMembersSideEffect.ShowBlockConfirmation)
        }
    }

    fun confirmKick() {
        val user = selectedUser ?: return
        viewModelScope.launch {
            val result = groupRepository.kickUser(_groupId, user.id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(members = state.members.filter { it.id != user.id })
                }
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar("Пользователь выгнан"))
            } else {
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar("Ошибка при выгонении"))
            }
            selectedUser = null
        }
    }

    fun confirmBlock() {
        val user = selectedUser ?: return
        viewModelScope.launch {
            val result = groupRepository.banUser(_groupId, user.id)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(members = state.members.filter { it.id != user.id })
                }
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar("Пользователь заблокирован"))
            } else {
                _sideEffect.emit(GroupMembersSideEffect.ShowSnackbar("Ошибка при блокировке"))
            }
            selectedUser = null
        }
    }
}
