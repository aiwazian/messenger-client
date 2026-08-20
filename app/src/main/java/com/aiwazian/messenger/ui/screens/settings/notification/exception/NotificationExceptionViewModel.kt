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

/**
 * Одно исключение целиком: переключить уведомления или снять исключение.
 *
 * Экран один и тот же для уже существующего исключения и для ещё не созданного:
 * различает их только hasException, а запрос на создание и на изменение на сервере
 * и так один.
 *
 * На сервер ничего не уходит, пока не нажата галочка в шапке: иначе уйти
 * с экрана, ничего не изменив, было бы невозможно.
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
    
    /** Что сейчас записано на сервере: с этим сверяется положение переключателя. */
    private var savedEnabled = true
    
    /** Несохранённый выбор пользователя: живёт только до нажатия галочки. */
    private var pendingEnabled: Boolean? = null
    
    /**
     * Чат известен только в рантайме, поэтому приходит сюда, а не в конструктор.
     * Повторный вызов с тем же чатом ничего не пересобирает и не теряет несохранённый
     * выбор.
     */
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
    
    /**
     * Положение переключателя берётся из исключения, а если его ещё нет — из самого
     * чата: там уже посчитана настройка категории, и экран открывается с тем
     * состоянием, которое для этого чата действует на самом деле.
     *
     * Пока переключатель не трогали, экран следует за кэшем; после — показывает
     * выбор пользователя, иначе обновление списка вернуло бы тумблер обратно.
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
                savedEnabled = state.notificationsEnabled
                
                _uiState.value = state.copy(
                    notificationsEnabled = pendingEnabled ?: state.notificationsEnabled
                )
            }
        }
    }
    
    /** Переключатель меняет только экран: на сервер выбор уедет по галочке. */
    fun toggleNotifications() {
        val enabled = !_uiState.value.notificationsEnabled
        
        pendingEnabled = enabled
        
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }
    
    /**
     * Галочка в шапке — единственное место, откуда изменения уходят на сервер.
     *
     * Исключения ещё нет — создаём его, даже если переключатель не трогали: за этим
     * экран и открывали из списка чатов. Существующее и нетронутое сохранять нечего,
     * просто уходим назад.
     */
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
    
    /**
     * Исключение снято — уходим назад. Список исключений читает тот же кэш
     * репозитория и обновится сам, возвращать результат не нужно.
     *
     * Удаление галочки не ждёт: это отдельное действие со своим подтверждением
     * в виде ухода с экрана.
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
