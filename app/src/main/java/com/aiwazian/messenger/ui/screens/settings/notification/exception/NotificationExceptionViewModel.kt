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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Одно исключение целиком: переключить уведомления или снять исключение.
 *
 * Экран один и тот же для уже существующего исключения и для ещё не созданного:
 * различает их только hasException, а запрос на создание и на изменение на сервере
 * и так один.
 */
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
    
    /**
     * Чат известен только в рантайме, поэтому приходит сюда, а не в конструктор.
     * Повторный вызов с тем же чатом ничего не пересобирает.
     */
    fun init(chatId: Long) {
        if (this.chatId == chatId) {
            return
        }
        
        this.chatId = chatId
        
        observeChat(chatId)
        
        viewModelScope.launch {
            notificationSettingsRepository.refreshChatExceptions()
        }
    }
    
    /**
     * Положение переключателя берётся из исключения, а если его ещё нет — из самого
     * чата: там уже посчитана настройка категории, и экран открывается с тем
     * состоянием, которое для этого чата действует на самом деле.
     */
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
                _uiState.value = state
            }
        }
    }
    
    /**
     * Одно нажатие — и для создания исключения, и для переключения уже
     * существующего: сервер в обоих случаях просто записывает новое значение.
     */
    fun toggleNotifications() {
        val chatId = chatId ?: return
        val enabled = !_uiState.value.notificationsEnabled
        
        viewModelScope.launch {
            notificationSettingsRepository.setChatNotifications(chatId, enabled).onFailure {
                _sideEffect.emit(
                    NotificationExceptionSideEffect.ShowSnackbar(R.string.notification_exception_update_failed)
                )
                
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    /**
     * Исключение снято — уходим назад. Список исключений читает тот же кэш
     * репозитория и обновится сам, возвращать результат не нужно.
     */
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
