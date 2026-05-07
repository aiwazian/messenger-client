/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupInviteLinkViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val vibrationManager: VibrationManager
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
            val maxUses = _uiState.value.maxUses.toIntOrNull()
            val expirationDate = _uiState.value.expirationDate
            
            groupRepository.createInviteLink(groupId, maxUses, expirationDate).onSuccess {
                _effect.emit(CreateGroupInviteLinkEffect.Success)
            }.onFailure {
                _effect.emit(CreateGroupInviteLinkEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
