/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.usecase.DownloadAvatarUseCase
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsProfileViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val uploadManager: UploadManager,
    private val downloadAvatarUseCase: DownloadAvatarUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<SettingsProfileSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    init {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(user = user) }
                loadProfileChannelName(user.profileChannelId)
                
                user.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                    .forEach { avatar ->
                        viewModelScope.launch {
                            downloadAvatarUseCase(user.id, avatar.fileId)
                                .onFailure {
                                    downloadingAvatars.remove(avatar.fileId)
                                    Log.e("SettingsProfileViewModel", "Error download avatar: ", it)
                                }
                        }
                    }
            }
        }
    }
    
    private fun loadProfileChannelName(channelId: Long?) {
        if (channelId == null) {
            _uiState.update { it.copy(profileChannelName = null) }
            return
        }
        viewModelScope.launch {
            channelRepository.getById(channelId).firstOrNull()?.let { channel ->
                _uiState.update { it.copy(profileChannelName = channel.name) }
            }
        }
    }
    
    fun setPendingAvatarUri(uri: Uri?) {
        _uiState.update { it.copy(pendingAvatarUri = uri) }
    }
    
    fun clearPendingAvatarUri() {
        _uiState.update { it.copy(pendingAvatarUri = null) }
    }
    
    fun onChangeFirstName(newName: String) {
        _uiState.update { it.copy(user = it.user.copy(firstName = newName)) }
    }
    
    fun onChangeLastName(newName: String) {
        _uiState.update { it.copy(user = it.user.copy(lastName = newName)) }
    }
    
    fun onChangeBio(newBio: String) {
        _uiState.update { it.copy(user = it.user.copy(bio = newBio)) }
    }
    
    fun onChangeDateOfBirth(newDate: Long?) {
        _uiState.update {
            it.copy(
                user = it.user.copy(dateOfBirth = newDate),
                showDatePicker = false
            )
        }
    }
    
    fun showDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }
    
    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }
    
    fun onSaveAndBack() {
        save()
        viewModelScope.launch {
            _sideEffect.emit(SettingsProfileSideEffect.NavigateBack)
        }
    }
    
    fun save() {
        viewModelScope.launch {
            userRepository.updateProfile(_uiState.value.user)
        }
    }
    
    fun deleteAvatar(fileId: String) {
        viewModelScope.launch {
            userRepository.deleteAvatar(fileId).onFailure {
                Log.e("SettingsProfileViewModel", "error delete avatar", it)
            }
        }
    }
    
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            userRepository.initUploadAvatar(
                uri.getFileName(context) ?: "",
                uri.getFileSize(context) ?: 0,
                uri.getFileType(context)
            ).onSuccess { uploadInfo ->
                uploadManager.upload(
                    fileUri = uri,
                    upload = uploadInfo,
                    fileId = uploadInfo.fileId
                ).onSuccess {
                    userRepository.confirmUploadAvatar(uploadInfo.fileId).onSuccess {
                        userRepository.addAvatarLocal(uploadInfo.fileId)
                    }.onFailure {
                        val error = if (it.isNetworkError()) {
                            UiText.StringResource(R.string.failed_to_connect)
                        } else {
                            UiText.StringResource(R.string.unexpected_error)
                        }
                        _sideEffect.emit(SettingsProfileSideEffect.ShowSnackbar(error))
                        Log.e("SettingsProfileViewModel", "error confirm", it)
                    }
                }.onFailure {
                    val error = if (it.isNetworkError()) {
                        UiText.StringResource(R.string.failed_to_connect)
                    } else {
                        UiText.StringResource(R.string.unexpected_error)
                    }
                    _sideEffect.emit(SettingsProfileSideEffect.ShowSnackbar(error))
                    Log.e("SettingsProfileViewModel", "error upload", it)
                }
            }.onFailure {
                val error = if (it.isNetworkError()) {
                    UiText.StringResource(R.string.failed_to_connect)
                } else {
                    UiText.StringResource(R.string.unexpected_error)
                }
                _sideEffect.emit(SettingsProfileSideEffect.ShowSnackbar(error))
                Log.e("SettingsProfileViewModel", "error initUploadAvatar", it)
            }
        }
    }
}
