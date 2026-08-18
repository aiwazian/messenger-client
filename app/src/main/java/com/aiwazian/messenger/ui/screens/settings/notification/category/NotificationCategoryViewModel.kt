/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.notification.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
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
class NotificationCategoryViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val chatRepository: ChatRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationCategoryUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<NotificationCategorySideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    private var category: ChatFolderCategory? = null
    private var observeJob: Job? = null
    
    /**
     * Экран открыт для конкретной категории, а она известна только в рантайме,
     * поэтому приходит сюда, а не через конструктор. Повторный вызов с той же
     * категорией (пересоздание экрана) ничего не пересобирает.
     */
    fun init(category: ChatFolderCategory) {
        if (this.category == category) return
        this.category = category
        observeExceptions(category)
        
        viewModelScope.launch {
            notificationSettingsRepository.refreshChatExceptions()
        }
    }
    
    /**
     * Список строится из общего кэша исключений и списка чатов: имя и аватар берём
     * из чата, а само исключение — из кэша. Пересобирается сам, когда исключение
     * убрали здесь или на другом устройстве.
     */
    private fun observeExceptions(category: ChatFolderCategory) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                notificationSettingsRepository.observeChatExceptions(),
                chatRepository.getAllChats()
            ) { exceptions, chats ->
                val chatsById = chats.associateBy { it.id }
                exceptions
                    .filter { category.matches(it.chatId) }
                    .map { exception ->
                        NotificationExceptionItem(
                            chatId = exception.chatId,
                            chat = chatsById[exception.chatId],
                            enabled = exception.enabled
                        )
                    }
            }.collect { items ->
                _uiState.update { it.copy(isLoading = false, exceptions = items) }
            }
        }
    }
    
    /**
     * Удалить одно исключение. Список обновится сам через кэш, здесь только
     * отчитываемся о результате.
     */
    fun removeException(chatId: Long) {
        viewModelScope.launch {
            notificationSettingsRepository.removeChatException(chatId)
                .onSuccess {
                    _sideEffect.emit(NotificationCategorySideEffect.ShowSnackbar(R.string.exception_deleted))
                }
                .onFailure {
                    _sideEffect.emit(NotificationCategorySideEffect.ShowSnackbar(R.string.failed_to_delete_exception))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
        }
    }
    
    fun showDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllDialog = true) }
    }
    
    fun hideDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllDialog = false) }
    }
    
    /** Удалить все исключения этой категории; чужие категории не трогаются. */
    fun removeAllExceptions() {
        val category = category ?: return
        
        viewModelScope.launch {
            hideDeleteAllDialog()
            notificationSettingsRepository.removeAllChatExceptions(category)
                .onSuccess {
                    _sideEffect.emit(NotificationCategorySideEffect.ShowSnackbar(R.string.all_exceptions_deleted))
                }
                .onFailure {
                    _sideEffect.emit(NotificationCategorySideEffect.ShowSnackbar(R.string.failed_to_delete_exceptions))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
        }
    }
}
