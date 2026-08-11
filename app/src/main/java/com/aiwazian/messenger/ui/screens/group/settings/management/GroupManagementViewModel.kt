/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.usecase.DeleteGroupUseCase
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
class GroupManagementViewModel @Inject constructor(
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupManagementUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupManagementEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        _uiState.update { it.copy(groupId = groupId) }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun delete() {
        viewModelScope.launch {
            val groupId = _uiState.value.groupId
            
            if (groupId <= 0) {
                return@launch
            }
            
            if (deleteGroupUseCase(groupId)) {
                _uiEffect.emit(GroupManagementEffect.NavigateToMain)
            } else {
                _uiState.update { it.copy(showDeleteDialog = false) }
                _uiEffect.emit(GroupManagementEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_delete_group)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
