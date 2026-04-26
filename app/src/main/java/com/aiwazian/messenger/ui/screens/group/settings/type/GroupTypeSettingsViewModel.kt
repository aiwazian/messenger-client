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
import com.aiwazian.messenger.ui.screens.channel.settings.type.LinkCheckStatus
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
            groupRepository.getById(groupId).collect { group ->
                _uiState.update {
                    it.copy(
                        groupId = groupId,
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
                canSave = canSaveGroupType(groupType, it.username, it.linkCheckStatus)
            )
        }
    }
    
    fun changePublicLink(publicLink: String) {
        _uiState.update {
            it.copy(
                username = publicLink,
                linkCheckStatus = LinkCheckStatus.Idle,
                canSave = canSaveGroupType(it.groupType, publicLink, LinkCheckStatus.Idle)
            )
        }
        
        checkLinkJob?.cancel()
        checkLinkJob = viewModelScope.launch {
            delay(500)
            checkPublicLinkAvailability(publicLink)
        }
    }
    
    private fun checkPublicLinkAvailability(publicLink: String) {
        if (publicLink.isBlank()) {
            _uiState.update {
                it.copy(
                    linkCheckStatus = LinkCheckStatus.Idle,
                    canSave = canSaveGroupType(it.groupType, publicLink, LinkCheckStatus.Idle)
                )
            }
            return
        }
        
        if (publicLink.length < 3) {
            _uiState.update {
                it.copy(
                    linkCheckStatus = LinkCheckStatus.Error("Минимальная длина 3 символа"),
                    canSave = false
                )
            }
            return
        }
        
        if (publicLink == _uiState.value.username) {
            _uiState.update {
                it.copy(linkCheckStatus = LinkCheckStatus.Available, canSave = true)
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(linkCheckStatus = LinkCheckStatus.Checking) }
            
            try {
                val isAvailable = searchRepository.checkUsernameAvailable(publicLink)
                _uiState.update {
                    it.copy(
                        linkCheckStatus = if (isAvailable) LinkCheckStatus.Available else LinkCheckStatus.Busy,
                        canSave = isAvailable
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(linkCheckStatus = LinkCheckStatus.Error("Ошибка проверки")) }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            if (!canSaveGroupType(
                    currentState.groupType,
                    currentState.username,
                    currentState.linkCheckStatus
                )
            ) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            try {
                groupRepository.updateGroupType(
                    currentState.groupId,
                    currentState.groupType,
                    currentState.username
                )
                    .onSuccess {
                        _uiEffect.emit(GroupTypeSettingsEffect.NavigateBack)
                    }
                    .onFailure {
                        vibrationManager.vibrate(VibrationPattern.Error)
                        _uiEffect.emit(GroupTypeSettingsEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
                    }
            } catch (_: Exception) {
                vibrationManager.vibrate(VibrationPattern.Error)
                _uiEffect.emit(GroupTypeSettingsEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
            }
        }
    }
    
    private fun canSaveGroupType(
        groupType: GroupType,
        publicLink: String,
        status: LinkCheckStatus
    ): Boolean {
        if (groupType == GroupType.PUBLIC) {
            return publicLink.isNotBlank() && status == LinkCheckStatus.Available
        }
        return true
    }
}
