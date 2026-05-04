/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Avatar
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileSize
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsProfileViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val uploadManager: UploadManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = Channel<SettingsProfileSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()
    
    init {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
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
        viewModelScope.launch {
            userRepository.updateProfile(_uiState.value.user)
            _sideEffect.send(SettingsProfileSideEffect.NavigateBack)
        }
    }
    
    fun deleteAvatar(fileId: String) {
        viewModelScope.launch {
            //            val success = userRepository.deleteAvatar(fileId)
            //            if (success) {
            // Refresh logic would go here
            //            }
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
                    uri = uri,
                    uploadUrl = uploadInfo.signedUrl,
                    fileId = uploadInfo.fileId
                ).onSuccess {
                    userRepository.confirmUploadAvatar(uploadInfo.fileId).onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                user = state.user.copy(
                                    avatars = state.user.avatars.plus(
                                        Avatar(
                                            uri = uri,
                                            fileId = uploadInfo.fileId,
                                            sortOrder = state.user.avatars.size + 1
                                        )
                                    )
                                )
                            )
                        }
                    }.onFailure {
                        Log.e("SettingsProfileViewModel", "error confirm", it)
                    }
                }.onFailure {
                    Log.e("SettingsProfileViewModel", "error upload", it)
                }
            }.onFailure {
                Log.e("SettingsProfileViewModel", "error initUploadAvatar", it)
            }
        }
    }
}
