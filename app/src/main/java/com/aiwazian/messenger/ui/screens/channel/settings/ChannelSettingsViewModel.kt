/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _channel = MutableStateFlow(Channel())
    val channelInfo = _channel.asStateFlow()
    
    private val _originalChannel = MutableStateFlow(Channel())
    
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges = _hasChanges.asStateFlow()

    val deleteDialog = DialogController()

    private var isInitialized = false

    fun init(channelId: Long) {
        if (isInitialized) return
        isInitialized = true
        _channelId = channelId
        
        viewModelScope.launch {
            if (channelId != -1L) {
                load()
            }
        }
    }

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun changeName(newName: String) {
        _channel.update { it.copy(name = newName) }
        updateHasChanges()
    }
    
    fun changeBio(newBio: String) {
        _channel.update { it.copy(bio = newBio) }
        updateHasChanges()
    }
    
    private fun updateHasChanges() {
        val current = _channel.value
        val original = _originalChannel.value
        _hasChanges.value = current.name != original.name ||
                (current.bio ?: "") != (original.bio ?: "") ||
                current.channelType != original.channelType ||
                (current.username ?: "") != (original.username ?: "")
    }
    
    suspend fun load() {
        // Фоновое сетевое обновление
        viewModelScope.launch {
            channelRepository.getById(_channelId).collect {}
        }

        // Реактивное обновление из Room
        channelRepository.getByIdFlow(_channelId).collect { channelInfo ->
            _channel.update { channelInfo }
            _originalChannel.update { channelInfo }
            _hasChanges.value = false
        }
    }
    
    suspend fun getSubscribers(search: String? = null): List<User> {
        val subscribers = channelRepository.getSubscribers(_channelId, search = search)
        return subscribers.getOrThrow()
    }
    
    suspend fun trySave(): Long? {
        if (!checkValid()) {
            vibrationManager.vibrate(VibrationPattern.Error)
            return null
        }
        
        val result = channelRepository.update(_channel.value)
        return if (result.isSuccess) {
            _originalChannel.value = _channel.value
            _hasChanges.value = false
            _channel.value.id
        } else {
            vibrationManager.vibrate(VibrationPattern.Error)
            null
        }
    }
    
    suspend fun tryDelete(): Boolean {
        return channelRepository.delete(channelInfo.value.id).isSuccess
    }

    private fun checkValid(): Boolean {
        if (_channel.value.name.isBlank()) {
            return false
        }
        
        if (_channel.value.channelType == ChannelType.PUBLIC.ordinal && _channel.value.username?.isBlank() == true) {
            return false
        }
        
        return true
    }

    fun kickUser(userId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = channelRepository.kickUser(_channelId, userId)
            onResult(result.isSuccess)
        }
    }
}
