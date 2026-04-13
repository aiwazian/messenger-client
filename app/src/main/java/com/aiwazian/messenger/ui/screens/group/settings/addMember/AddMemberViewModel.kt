/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.addMember

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class AddMemberViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private var _groupId: Long = -1L

    private val _uiState = MutableStateFlow(AddMemberState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AddMemberSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun init(groupId: Long) {
        _groupId = groupId
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupRepository.getAvailableUsersForInvite(_groupId).collectLatest { users ->
                _uiState.update { it.copy(users = users, isLoading = false) }
            }
        }
    }

    fun toggleUser(userId: Long) {
        _uiState.update { state ->
            val newSet = if (state.selectedUserIds.contains(userId)) {
                state.selectedUserIds - userId
            } else {
                state.selectedUserIds + userId
            }
            state.copy(selectedUserIds = newSet)
        }
    }

    fun addSelectedUsers(onDone: () -> Unit) {
        val selectedIds = _uiState.value.selectedUserIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val result = groupRepository.addMembers(_groupId, selectedIds.toList())
            if (result.isSuccess) {
                _sideEffect.emit(AddMemberSideEffect.ShowSnackbar("Участники добавлены"))
                onDone()
            } else {
                _sideEffect.emit(AddMemberSideEffect.ShowSnackbar("Ошибка при добавлении"))
            }
        }
    }
}

sealed interface AddMemberSideEffect {
    data class ShowSnackbar(val message: String) : AddMemberSideEffect
}
