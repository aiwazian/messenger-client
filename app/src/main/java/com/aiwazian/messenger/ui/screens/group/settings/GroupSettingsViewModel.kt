/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.usecase.DeleteGroupUseCase
import com.aiwazian.messenger.usecase.DownloadAvatarUseCase
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.UploadManager
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
    private val groupRepository: GroupRepository,
    private val groupAdminsRepository: GroupAdminsRepository,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val vibrationManager: VibrationManager,
    private val uploadManager: UploadManager,
    private val downloadAvatarUseCase: DownloadAvatarUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupSettingsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            loadMyPermissions(groupId)
            loadCounters(groupId)
        }
        
        viewModelScope.launch {
            groupRepository.fetchById(groupId)
            groupRepository.getById(groupId).collectLatest { group ->
                _uiState.update { it.copy(group = group, originalChannelData = group) }
                
                group.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                    .forEach { avatar ->
                        viewModelScope.launch {
                            downloadAvatarUseCase(groupId, avatar.fileId)
                                .onFailure {
                                    downloadingAvatars.remove(avatar.fileId)
                                }
                        }
                    }
            }
        }
    }
    
    private suspend fun loadMyPermissions(groupId: Long) {
        groupAdminsRepository.getMyPermissions(groupId).onSuccess { permissions ->
            _uiState.update { it.copy(permissions = permissions) }
        }.onFailure { error ->
            Log.e(TAG, "error load my permissions", error)
            _uiState.update { it.copy(permissions = ChatAdminPermissions()) }
        }
    }
    
    /** Счётчики грузятся только для тех блоков, которые видны текущему пользователю. */
    private suspend fun loadCounters(groupId: Long) {
        val state = _uiState.value
        
        if (state.canManageAdmins) {
            groupAdminsRepository.getAdmins(groupId).onSuccess { admins ->
                _uiState.update { it.copy(adminsCount = admins.size) }
            }.onFailure { error ->
                Log.e(TAG, "error load admins count", error)
            }
        }
        
        if (state.isOwner) {
            groupRepository.getJoinRequests(groupId).onSuccess { requests ->
                _uiState.update { it.copy(joinRequestsCount = requests.size) }
            }.onFailure { error ->
                Log.e(TAG, "error load join requests count", error)
            }
        }
    }
    
    fun setPendingAvatarUri(uri: Uri?) {
        _uiState.update { it.copy(pendingAvatarUri = uri) }
    }
    
    fun clearPendingAvatarUri() {
        _uiState.update { it.copy(pendingAvatarUri = null) }
    }
    
    fun deleteAvatar(fileId: String) {
        viewModelScope.launch {
            groupRepository.deleteAvatar(_uiState.value.group.id, fileId).onFailure {
                Log.e(TAG, "error delete avatar", it)
            }
        }
    }
    
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val groupId = _uiState.value.group.id
            groupRepository.initUploadAvatar(
                groupId,
                uri.getFileName(context) ?: "",
                uri.getFileSize(context) ?: 0,
                uri.getFileType(context)
            ).onSuccess { uploadInfo ->
                uploadManager.upload(
                    fileUri = uri,
                    uploadUrl = uploadInfo.signedUrl,
                    fileId = uploadInfo.fileId
                ).onSuccess {
                    groupRepository.confirmUploadAvatar(groupId, uploadInfo.fileId).onFailure {
                        val error = if (it.isNetworkError()) {
                            UiText.StringResource(R.string.failed_to_connect)
                        } else {
                            UiText.StringResource(R.string.unexpected_error)
                        }
                        _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(error))
                    }
                }.onFailure {
                    val error = if (it.isNetworkError()) {
                        UiText.StringResource(R.string.failed_to_connect)
                    } else {
                        UiText.StringResource(R.string.unexpected_error)
                    }
                    _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(error))
                }
            }.onFailure {
                val error = if (it.isNetworkError()) {
                    UiText.StringResource(R.string.failed_to_connect)
                } else {
                    UiText.StringResource(R.string.unexpected_error)
                }
                _uiEffect.emit(GroupSettingsUiEffect.ShowSnackbar(error))
            }
        }
    }
    
    fun changeName(newName: String) {
        if (!_uiState.value.canEditProfile) return
        _uiState.update { it.copy(group = it.group.copy(name = newName)) }
        updateHasChanges()
    }
    
    fun changeBio(newBio: String) {
        if (!_uiState.value.canEditProfile) return
        _uiState.update { it.copy(group = it.group.copy(bio = newBio)) }
        updateHasChanges()
    }
    
    private fun updateHasChanges() {
        _uiState.update { it.copy(hasChanges = _uiState.value.group != _uiState.value.originalChannelData) }
    }
    
    fun save() {
        viewModelScope.launch {
            if (!_uiState.value.canEditProfile) {
                return@launch
            }
            
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
            if (!_uiState.value.isOwner) {
                return@launch
            }
            
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
    
    private companion object {
        const val TAG = "GroupSettingsViewModel"
    }
}
