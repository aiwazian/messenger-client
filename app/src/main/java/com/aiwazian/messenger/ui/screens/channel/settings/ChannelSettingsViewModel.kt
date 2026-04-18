/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelSettingsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private var _channelId: Long = -1L
    
    private val _uiState = MutableStateFlow(ChannelSettingsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ChannelSettingsEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _channelId = channelId
        
        viewModelScope.launch {
            if (channelId != -1L) {
                channelRepository.getByIdFlow(_channelId).collect { channelInfo ->
                    _uiState.update {
                        it.copy(
                            channel = channelInfo,
                            originalChannelData = channelInfo
                        )
                    }
                }
            }
        }
    }
    
    fun changeName(newName: String) {
        _uiState.update { it.copy(channel = it.channel.copy(name = newName)) }
        updateHasChanges()
    }
    
    fun changeBio(newBio: String) {
        _uiState.update { it.copy(channel = it.channel.copy(bio = newBio)) }
        updateHasChanges()
    }
    
    private fun updateHasChanges() {
        _uiState.update { it.copy(hasChanges = _uiState.value.channel != _uiState.value.originalChannelData) }
    }
    
    fun save() {
        viewModelScope.launch {
            if (!checkValid()) {
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            channelRepository.update(_uiState.value.channel).onSuccess {
                _uiState.update { it.copy(originalChannelData = _uiState.value.channel) }
                _uiEffect.emit(ChannelSettingsEffect.NavigateToBack)
            }.onFailure {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar("Не удалось сохранить изменения"))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun delete() {
        viewModelScope.launch {
            channelRepository.delete(uiState.value.channel.id).onSuccess {
                _uiEffect.emit(ChannelSettingsEffect.NavigateToMain)
            }.onFailure {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar("Не удалось удалить канал"))
            }
        }
    }
    
    private fun checkValid(): Boolean {
        if (_uiState.value.channel.name.isBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar("Введите название канала"))
            }
            return false
        }
        
        if (_uiState.value.channel.channelType == ChannelType.PUBLIC && _uiState.value.channel.username.isNullOrBlank()) {
            viewModelScope.launch {
                _uiEffect.emit(ChannelSettingsEffect.ShowSnackbar("Введите публичную ссылку канала"))
            }
            return false
        }
        
        return true
    }
    
    fun unbanUser(userId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = channelRepository.unbanUser(_channelId, userId)
            onResult(result.isSuccess)
        }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    suspend fun getBannedUsers(search: String? = null): List<User> {
        val bannedUsers = channelRepository.getBannedUsers(_channelId, search = search)
        return bannedUsers.getOrThrow()
    }
}
