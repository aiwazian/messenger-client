/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupSettingsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var _groupId: Long = -1L
    
    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupSettingsUiEffect>()
    val uiEffect: SharedFlow<GroupSettingsUiEffect> = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        if (_groupId != -1L) return
        _groupId = groupId
        
        viewModelScope.launch {
            loadGroup(groupId)
        }
    }
    
    private fun loadGroup(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupRepository.getById(groupId).collectLatest { group ->
                group?.let {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            group = group
                        )
                    }
                }
            }
        }
    }
    
    fun changeGroupName(newName: String) {
        _uiState.update {
            it.copy(group = it.group.copy(name = newName))
        }
    }
    
    fun changeGroupBio(newBio: String) {
        _uiState.update {
            it.copy(group = it.group.copy(bio = newBio))
        }
    }
    
    fun saveGroup() {
        viewModelScope.launch {
            val group = _uiState.value.group
            
            if (group.name.isBlank()) {
                _uiEffect.emit(GroupSettingsUiEffect.ShowError("Введите название группы"))
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = groupRepository.update(group)
            result.fold(
                onSuccess = {
                    _uiEffect.emit(GroupSettingsUiEffect.NavigateBack)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Ошибка при обновлении группы"
                        )
                    }
                    _uiEffect.emit(GroupSettingsUiEffect.ShowError(exception.message ?: "Ошибка"))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            )
        }
    }
    
    fun deleteGroup() {
        viewModelScope.launch {
            val group = _uiState.value.group
            
            _uiState.update { it.copy(isDeleting = true, error = null) }
            
            val result = groupRepository.delete(group.id)
            result.fold(
                onSuccess = {
                    _uiEffect.emit(GroupSettingsUiEffect.NavigateToMain)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = exception.message ?: "Ошибка при удалении группы"
                        )
                    }
                    _uiEffect.emit(GroupSettingsUiEffect.ShowError(exception.message ?: "Ошибка"))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            )
        }
    }
}
