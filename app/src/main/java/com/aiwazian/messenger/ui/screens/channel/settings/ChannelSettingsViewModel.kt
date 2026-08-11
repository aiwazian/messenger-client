/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
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
class ChannelSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val channelRepository: ChannelRepository,
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val vibrationManager: VibrationManager,
    private val uploadManager: UploadManager,
    private val downloadAvatarUseCase: DownloadAvatarUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChannelSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    fun init(channelId: Long) {
        viewModelScope.launch {
            loadMyPermissions(channelId)
            loadCounters(channelId)
        }
        
        viewModelScope.launch {
            channelRepository.fetchById(channelId)
            channelRepository.getById(channelId).collectLatest { channel ->
                _uiState.update { it.copy(channel = channel, originalChannelData = channel) }
                
                channel.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                    .forEach { avatar ->
                        viewModelScope.launch {
                            downloadAvatarUseCase(channelId, avatar.fileId)
                                .onFailure {
                                    downloadingAvatars.remove(avatar.fileId)
                                }
                        }
                    }
            }
        }
    }
    
    private suspend fun loadMyPermissions(channelId: Long) {
        channelAdminsRepository.getMyPermissions(channelId).onSuccess { permissions ->
            _uiState.update { it.copy(permissions = permissions) }
        }.onFailure { error ->
            Log.e(TAG, "error load my permissions", error)
            _uiState.update { it.copy(permissions = ChatAdminPermissions()) }
        }
    }
    
    private suspend fun loadCounters(channelId: Long) {
        val state = _uiState.value
        
        if (state.canManageAdmins) {
            channelAdminsRepository.getAdmins(channelId).onSuccess { admins ->
                _uiState.update { it.copy(adminsCount = admins.size) }
            }.onFailure { error ->
                Log.e(TAG, "error load admins count", error)
            }
        }
        
        if (state.isOwner) {
            channelRepository.getJoinRequests(channelId).onSuccess { requests ->
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
            channelRepository.deleteAvatar(_uiState.value.channel.id, fileId).onFailure {
                Log.e(TAG, "error delete avatar", it)
            }
        }
    }
    
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val channelId = _uiState.value.channel.id
            channelRepository.initUploadAvatar(
                channelId,
                uri.getFileName(context) ?: "",
                uri.getFileSize(context) ?: 0,
                uri.getFileType(context)
            ).onSuccess { uploadInfo ->
                uploadManager.upload(
                    fileUri = uri,
                    upload = uploadInfo,
                    fileId = uploadInfo.fileId
                ).onSuccess {
                    channelRepository.confirmUploadAvatar(channelId, uploadInfo.fileId).onFailure {
                        val error = if (it.isNetworkError()) {
                            UiText.StringResource(R.string.failed_to_connect)
                        } else {
                            UiText.StringResource(R.string.unexpected_error)
                        }
                        _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(error))
                    }
                }.onFailure {
                    val error = if (it.isNetworkError()) {
                        UiText.StringResource(R.string.failed_to_connect)
                    } else {
                        UiText.StringResource(R.string.unexpected_error)
                    }
                    _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(error))
                }
            }.onFailure {
                val error = if (it.isNetworkError()) {
                    UiText.StringResource(R.string.failed_to_connect)
                } else {
                    UiText.StringResource(R.string.unexpected_error)
                }
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(error))
            }
        }
    }
    
    fun changeName(newName: String) {
        if (!_uiState.value.canEditProfile) return
        _uiState.update { it.copy(channel = it.channel.copy(name = newName)) }
        updateHasChanges()
    }
    
    fun changeBio(newBio: String) {
        if (!_uiState.value.canEditProfile) return
        _uiState.update { it.copy(channel = it.channel.copy(bio = newBio)) }
        updateHasChanges()
    }
    
    private fun updateHasChanges() {
        _uiState.update { it.copy(hasChanges = _uiState.value.channel != _uiState.value.originalChannelData) }
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
            
            channelRepository.update(_uiState.value.channel).onSuccess {
                _uiState.update { it.copy(originalChannelData = _uiState.value.channel) }
                _uiEffect.emit(ChannelSettingsEffect.NavigateToBack)
            }.onFailure {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private fun checkValid(): Boolean {
        if (_uiState.value.channel.name.isBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(UiText.StringResource(R.string.error_empty_channel_name)))
            }
            return false
        }
        
        if (_uiState.value.channel.channelType == ChannelType.PUBLIC && _uiState.value.channel.username.isNullOrBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar(UiText.StringResource(R.string.error_empty_public_link)))
            }
            return false
        }
        
        return true
    }
    
    private companion object {
        const val TAG = "ChannelSettingsViewModel"
    }
}
