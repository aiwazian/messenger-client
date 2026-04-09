/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.ui.screens.channel.settings.type.LinkCheckStatus
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupTypeSettingsUiState(
    val isLoading: Boolean = false,
    val group: com.aiwazian.messenger.domain.Group? = null,
    val groupType: GroupType = GroupType.PRIVATE,
    val username: String = "",
    val canSave: Boolean = false,
    val linkCheckStatus: LinkCheckStatus = LinkCheckStatus.Idle,
    val error: String? = null
)

@HiltViewModel
class GroupTypeSettingsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val searchRepository: SearchRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var _groupId: Long = -1L
    private var checkLinkJob: Job? = null
    
    private val _uiState = MutableStateFlow(GroupTypeSettingsUiState())
    val uiState: StateFlow<GroupTypeSettingsUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupTypeSettingsEffect>()
    val uiEffect: SharedFlow<GroupTypeSettingsEffect> = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        _groupId = groupId
        loadGroup(groupId)
    }
    
    private fun loadGroup(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupRepository.getById(groupId).collect { groupInfo ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        group = groupInfo,
                        groupType = groupInfo.groupType,
                        username = groupInfo.username ?: "",
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
                    canSave = canSaveGroupType(
                        it.groupType,
                        publicLink,
                        LinkCheckStatus.Idle
                    )
                )
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
            } catch (e: Exception) {
                _uiState.update { it.copy(linkCheckStatus = LinkCheckStatus.Error("Ошибка проверки")) }
            }
        }
    }
    
    fun save() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val group = currentState.group ?: return@launch
            
            try {
                val updatedGroup = group.copy(
                    groupType = currentState.groupType,
                    username = if (currentState.groupType == GroupType.PUBLIC) currentState.username else null
                )
                val result = groupRepository.update(updatedGroup)
                if (result.isSuccess) {
                    _uiEffect.emit(GroupTypeSettingsEffect.NavigateBack)
                } else {
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                vibrationManager.vibrate(VibrationPattern.Error)
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
