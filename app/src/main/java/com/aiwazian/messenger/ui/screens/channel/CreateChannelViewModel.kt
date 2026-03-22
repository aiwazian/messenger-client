/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _channel = MutableStateFlow(Channel())
    val channelInfo = _channel.asStateFlow()

    private val _createState = MutableStateFlow<CreateChannelState>(CreateChannelState.Idle)
    val createState: StateFlow<CreateChannelState> = _createState.asStateFlow()

    private val _createEffect = MutableSharedFlow<CreateChannelEffect>()
    val createEffect: SharedFlow<CreateChannelEffect> = _createEffect.asSharedFlow()

    fun changeName(newName: String) {
        _channel.update { it.copy(name = newName) }
    }
    
    fun changeBio(newBio: String) {
        _channel.update { it.copy(bio = newBio) }
    }
    
    fun createChannel() {
        viewModelScope.launch {
            if (!checkValid()) {
                _createState.value = CreateChannelState.Error("Заполните название канала")
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _createState.value = CreateChannelState.Loading
            
            try {
                val result = channelRepository.create(_channel.value)
                
                result.fold(
                    onSuccess = { createdId ->
                        val channelName = _channel.value.name
                        _createState.value = CreateChannelState.Success(createdId, channelName)
                        
                        val chat = Chat(
                            id = createdId,
                            chatName = channelName
                        )
                        _createEffect.emit(CreateChannelEffect.NavigateToChat(chat))
                    },
                    onFailure = { exception ->
                        _createState.value = CreateChannelState.Error(
                            exception.message ?: "Ошибка при создании канала"
                        )
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                )
            } catch (e: Exception) {
                _createState.value = CreateChannelState.Error(
                    e.message ?: "Неизвестная ошибка"
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private fun checkValid(): Boolean {
        return _channel.value.name.isNotBlank()
    }
}
