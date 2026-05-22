/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupTypeSettingsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val searchRepository: SearchRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var checkLinkJob: Job? = null
    
    private val _uiState = MutableStateFlow(GroupTypeSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupTypeSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            groupRepository.fetchById(groupId)
            groupRepository.getById(groupId).firstOrNull()?.let { group ->
                _uiState.update {
                    it.copy(
                        groupId = group.id,
                        groupType = group.groupType,
                        username = group.username.orEmpty(),
                        canSave = true
                    )
                }
            }
        }
    }
    
    fun changeGroupType(groupType: GroupType) {
        _uiState.update {
            it.copy(
                groupType = groupType,
                canSave = groupType == GroupType.PRIVATE
            )
        }
    }
    
    fun onChangeUsername(newUsername: String) {
        val filteredUsername =
            newUsername.filter { it.toString().matches(RegexPatterns.SET_USERNAME) }
        _uiState.update { it.copy(username = filteredUsername) }
        
        if (filteredUsername.isBlank()) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        if (filteredUsername.length < 5) {
            _uiState.update {
                it.copy(
                    isError = true,
                    canSave = false,
                    statusText = UiText.StringResource(R.string.min_length_5_characters),
                )
            }
            return
        }
        
        if (filteredUsername == _uiState.value.originalName) {
            _uiState.update { it.copy(isError = false, canSave = true, statusText = null) }
            return
        }
        
        _uiState.update {
            it.copy(
                isError = false,
                canSave = false,
                statusText = UiText.DynamicString("Проверка")
            )
        }
        
        checkLinkJob?.cancel()
        checkLinkJob = viewModelScope.launch {
            delay(500)
            searchRepository.checkUsernameAvailable(filteredUsername).onSuccess { available ->
                if (available) {
                    _uiState.update {
                        it.copy(
                            canSave = true,
                            isError = false,
                            statusText = UiText.StringResource(R.string.username_available)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            canSave = false,
                            isError = true,
                            statusText = UiText.StringResource(R.string.username_taken)
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isError = true,
                        canSave = false,
                        statusText = UiText.StringResource(R.string.unexpected_error)
                    )
                }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            if (_uiState.value.username == _uiState.value.originalName) {
                _uiEffect.emit(GroupTypeSettingsEffect.NavigateBack)
                return@launch
            }
            
            val currentState = _uiState.value
            groupRepository.updateGroupType(
                currentState.groupId,
                currentState.groupType,
                currentState.username
            ).onSuccess {
                _uiEffect.emit(GroupTypeSettingsEffect.NavigateBack)
            }.onFailure {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(
                    GroupTypeSettingsEffect.ShowSnackbar
                        (
                        UiText.StringResource(R.string.failed_to_save_changes)
                    )
                )
            }
        }
    }
}
