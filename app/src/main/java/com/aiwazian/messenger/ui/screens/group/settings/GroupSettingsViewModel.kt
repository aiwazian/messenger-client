/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.usecase.DeleteGroupUseCase
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
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
class GroupSettingsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupSettingsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            groupRepository.getById(groupId).collectLatest { group ->
                _uiState.update { it.copy(group = group, originalChannelData = group) }
            }
        }
    }
    
    fun changeName(newName: String) {
        _uiState.update { it.copy(group = it.group.copy(name = newName)) }
        updateHasChanges()
    }
    
    fun changeBio(newBio: String) {
        _uiState.update { it.copy(group = it.group.copy(bio = newBio)) }
        updateHasChanges()
    }
    
    private fun updateHasChanges() {
        _uiState.update { it.copy(hasChanges = _uiState.value.group != _uiState.value.originalChannelData) }
    }
    
    fun save() {
        viewModelScope.launch {
            if (!checkValid()) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            groupRepository.update(_uiState.value.group).onSuccess {
                _uiEffect.emit(GroupSettingsUiEffect.NavigateBack)
            }.onFailure {
                _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun delete() {
        viewModelScope.launch {
            if (deleteGroupUseCase(_uiState.value.group.id)) {
                _uiEffect.emit(GroupSettingsUiEffect.NavigateToMain)
            } else {
                _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_delete_group)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    private fun checkValid(): Boolean {
        if (_uiState.value.group.name.isBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(UiText.StringResource(R.string.error_empty_channel_name)))
            }
            return false
        }
        
        if (_uiState.value.group.groupType == GroupType.PUBLIC && _uiState.value.group.username.isNullOrBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(UiText.StringResource(R.string.error_empty_public_link)))
            }
            return false
        }
        
        return true
    }
}
