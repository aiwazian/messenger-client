/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupInviteLinkUiState(
    val maxUses: String = "",
    val expirationDate: Long? = null,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface CreateGroupInviteLinkEffect {
    object Success : CreateGroupInviteLinkEffect
}

@HiltViewModel
class CreateGroupInviteLinkViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupInviteLinkUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CreateGroupInviteLinkEffect>()
    val effect = _effect.asSharedFlow()

    private var groupId: Long = -1

    fun init(groupId: Long) {
        this.groupId = groupId
    }

    fun onMaxUsesChange(value: String) {
        _uiState.update { it.copy(maxUses = value) }
    }

    fun onExpirationDateChange(date: Long?) {
        _uiState.update { it.copy(expirationDate = date, showDatePicker = false) }
    }

    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun createLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val maxUses = _uiState.value.maxUses.toIntOrNull()
            val expirationDate = _uiState.value.expirationDate
            
            val expiresInSeconds = if (expirationDate != null) {
                ((expirationDate - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            } else null
            
            val result = groupRepository.createInviteLink(groupId, maxUses, expiresInSeconds)
            
            if (result.isSuccess) {
                _effect.emit(CreateGroupInviteLinkEffect.Success)
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
