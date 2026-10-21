/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.exception

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationExceptionViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val chatRepository: ChatRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationExceptionUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<NotificationExceptionSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private var chatId: Long? = null
    
    private var observeJob: Job? = null
    
    private var savedEnabled = true
    
    private var pendingEnabled: Boolean? = null
    
    fun init(chatId: Long) {
        if (this.chatId == chatId) {
            return
        }
        
        this.chatId = chatId
        pendingEnabled = null
        
        observeChat(chatId)
        
        viewModelScope.launch {
            notificationSettingsRepository.refreshChatExceptions()
        }
    }

    private fun observeChat(chatId: Long) {
        observeJob?.cancel()
        
        observeJob = viewModelScope.launch {
            combine(
                notificationSettingsRepository.observeChatExceptions(),
                notificationSettingsRepository.observeChatMuted(chatId),
                chatRepository.getAllChats()
            ) { exceptions, isMuted, chats ->
                val exception = exceptions.firstOrNull { it.chatId == chatId }
                
                NotificationExceptionUiState(
                    chat = chats.firstOrNull { it.id == chatId },
                    notificationsEnabled = exception?.enabled ?: !isMuted,
                    hasException = exception != null
                )
            }.collect { state ->
                savedEnabled = state.notificationsEnabled
                
                _uiState.value = state.copy(
                    notificationsEnabled = pendingEnabled ?: state.notificationsEnabled
                )
            }
        }
    }
    
    fun toggleNotifications() {
        val enabled = !_uiState.value.notificationsEnabled
        
        pendingEnabled = enabled
        
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun save() {
        val chatId = chatId ?: return
        val state = _uiState.value
        val enabled = state.notificationsEnabled
        
        viewModelScope.launch {
            if (state.hasException && enabled == savedEnabled) {
                _sideEffect.emit(NotificationExceptionSideEffect.NavigateBack)
                return@launch
            }
            
            notificationSettingsRepository.setChatNotifications(chatId, enabled).onSuccess {
                pendingEnabled = null
                
                _sideEffect.emit(NotificationExceptionSideEffect.NavigateBack)
            }.onFailure {
                _sideEffect.emit(
                    NotificationExceptionSideEffect.ShowSnackbar(R.string.notification_exception_update_failed)
                )
                
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }

    fun removeException() {
        val chatId = chatId ?: return
        
        viewModelScope.launch {
            notificationSettingsRepository.removeChatException(chatId).onSuccess {
                _sideEffect.emit(NotificationExceptionSideEffect.NavigateBack)
            }.onFailure {
                _sideEffect.emit(
                    NotificationExceptionSideEffect.ShowSnackbar(R.string.failed_to_delete_exception)
                )
                
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
