/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.transfer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupTransferOwnershipViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupTransferOwnershipUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupTransferOwnershipEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        _uiState.update { it.copy(groupId = groupId) }
        
        viewModelScope.launch {
            groupRepository.getMembers(groupId).onCompletion {
                _uiState.update { state -> state.copy(isLoading = false) }
            }.collect { members ->
                _uiState.update { state -> state.copy(members = members) }
            }
        }
    }
    
    fun selectUser(user: User) {
        _uiState.update { it.copy(selectedUser = user) }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedUser = null) }
    }
    
    fun confirmTransfer() {
        val state = _uiState.value
        val user = state.selectedUser ?: return
        
        if (state.isTransferring) {
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isTransferring = true) }
            
            groupRepository.transferOwnership(state.groupId, user.id).onSuccess {
                _uiState.update { it.copy(isTransferring = false, selectedUser = null) }
                _uiEffect.emit(GroupTransferOwnershipEffect.NavigateToMain)
            }.onFailure { error ->
                Log.e(TAG, "error transfer ownership", error)
                _uiState.update { it.copy(isTransferring = false, selectedUser = null) }
                _uiEffect.emit(
                    GroupTransferOwnershipEffect.ShowSnackbar(
                        UiText.StringResource(R.string.transfer_ownership_failed)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private companion object {
        const val TAG = "GroupTransferOwnershipViewModel"
    }
}
