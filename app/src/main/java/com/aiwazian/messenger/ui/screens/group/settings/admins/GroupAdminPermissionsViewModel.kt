/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Разрешения одного администратора группы.
 *
 * Экран открывается и при назначении нового администратора, и при изменении прав существующего:
 * во втором случае текущие значения подтягиваются из списка администраторов.
 */
@HiltViewModel
class GroupAdminPermissionsViewModel @Inject constructor(
    private val groupAdminsRepository: GroupAdminsRepository
) : ViewModel() {
    
    private var _groupId = -1L
    private var _userId = -1L
    
    private val _uiState = MutableStateFlow(GroupAdminPermissionsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<GroupAdminPermissionsSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun init(groupId: Long, userId: Long) {
        _groupId = groupId
        _userId = userId
        
        viewModelScope.launch {
            groupAdminsRepository.getAdmins(groupId).onSuccess { admins ->
                val admin = admins.firstOrNull { it.userId == userId } ?: return@onSuccess
                _uiState.update {
                    it.copy(
                        canManageInviteLinks = admin.canManageInviteLinks,
                        canEditProfile = admin.canEditProfile,
                        tag = admin.tag.orEmpty()
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Error loading group admin permissions", error)
            }
        }
    }
    
    fun toggleManageInviteLinks() {
        _uiState.update { it.copy(canManageInviteLinks = !it.canManageInviteLinks) }
    }
    
    fun toggleEditProfile() {
        _uiState.update { it.copy(canEditProfile = !it.canEditProfile) }
    }
    
    fun changeTag(tag: String) {
        _uiState.update { it.copy(tag = tag) }
    }
    
    fun save() {
        if (_uiState.value.isSaving) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val state = _uiState.value
            
            groupAdminsRepository.upsertAdmin(
                groupId = _groupId,
                userId = _userId,
                canManageInviteLinks = state.canManageInviteLinks,
                canEditProfile = state.canEditProfile,
                tag = state.tag
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _sideEffect.emit(GroupAdminPermissionsSideEffect.NavigateBack)
            }.onFailure { error ->
                Log.e(TAG, "Error saving group admin permissions", error)
                _uiState.update { it.copy(isSaving = false) }
                _sideEffect.emit(
                    GroupAdminPermissionsSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes))
                )
            }
        }
    }
    
    private companion object {
        const val TAG = "GroupAdminPermissionsViewModel"
    }
}
