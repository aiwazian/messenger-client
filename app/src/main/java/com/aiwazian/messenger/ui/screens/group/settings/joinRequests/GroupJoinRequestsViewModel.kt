/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.joinRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupJoinRequestsViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupJoinRequestsState())
    val uiState: StateFlow<GroupJoinRequestsState> = _uiState.asStateFlow()
    
    private val _effect = MutableSharedFlow<GroupJoinRequestsEffect>()
    val effect: SharedFlow<GroupJoinRequestsEffect> = _effect.asSharedFlow()
    
    private var groupId: Long = 0
    
    fun init(id: Long) {
        groupId = id
        loadRequests()
    }
    
    private fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = groupRepository.getJoinRequests(groupId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        requests = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(GroupJoinRequestsEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
            }
        }
    }
    
    fun acceptRequest(userId: Long) {
        viewModelScope.launch {
            val result = groupRepository.acceptJoinRequest(groupId, userId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(requests = state.requests.filter { it.id != userId })
                }
            } else {
                _effect.emit(GroupJoinRequestsEffect.ShowSnackbar(UiText.DynamicString("Ошибка при принятии заявки")))
            }
        }
    }
    
    fun rejectRequest(userId: Long) {
        viewModelScope.launch {
            val result = groupRepository.rejectJoinRequest(groupId, userId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(requests = state.requests.filter { it.id != userId })
                }
            } else {
                _effect.emit(GroupJoinRequestsEffect.ShowSnackbar(UiText.DynamicString("Ошибка при отклонении заявки")))
            }
        }
    }
}
