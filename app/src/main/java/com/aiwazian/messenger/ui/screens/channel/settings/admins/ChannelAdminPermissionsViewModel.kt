/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Разрешения одного администратора канала. Тегов в каналах нет. */
@HiltViewModel
class ChannelAdminPermissionsViewModel @Inject constructor(
    private val channelAdminsRepository: ChannelAdminsRepository
) : ViewModel() {
    
    private var _channelId = -1L
    private var _userId = -1L
    
    private val _uiState = MutableStateFlow(ChannelAdminPermissionsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<ChannelAdminPermissionsSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun init(channelId: Long, userId: Long) {
        _channelId = channelId
        _userId = userId
        
        viewModelScope.launch {
            channelAdminsRepository.getAdmins(channelId).onSuccess { admins ->
                val admin = admins.firstOrNull { it.userId == userId } ?: return@onSuccess
                _uiState.update {
                    it.copy(
                        canManageInviteLinks = admin.canManageInviteLinks,
                        canEditProfile = admin.canEditProfile,
                        canManageAdmins = admin.canManageAdmins
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Error loading channel admin permissions", error)
            }
        }
    }
    
    fun toggleManageInviteLinks() {
        _uiState.update { it.copy(canManageInviteLinks = !it.canManageInviteLinks) }
    }
    
    fun toggleEditProfile() {
        _uiState.update { it.copy(canEditProfile = !it.canEditProfile) }
    }
    
    /** Право на управление администраторами выдаёт только владелец: это проверяет сервер. */
    fun toggleManageAdmins() {
        _uiState.update { it.copy(canManageAdmins = !it.canManageAdmins) }
    }
    
    fun save() {
        if (_uiState.value.isSaving) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val state = _uiState.value
            
            channelAdminsRepository.upsertAdmin(
                channelId = _channelId,
                userId = _userId,
                canManageInviteLinks = state.canManageInviteLinks,
                canEditProfile = state.canEditProfile,
                canManageAdmins = state.canManageAdmins
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _sideEffect.emit(ChannelAdminPermissionsSideEffect.NavigateBack)
            }.onFailure { error ->
                Log.e(TAG, "Error saving channel admin permissions", error)
                _uiState.update { it.copy(isSaving = false) }
                _sideEffect.emit(
                    ChannelAdminPermissionsSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes))
                )
            }
        }
    }
    
    private companion object {
        const val TAG = "ChannelAdminPermissionsViewModel"
    }
}
