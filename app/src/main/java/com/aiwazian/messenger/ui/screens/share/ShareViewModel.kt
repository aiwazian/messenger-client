/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.usecase.GetShareTargetsUseCase
import com.aiwazian.messenger.utils.MessageSendQueue
import com.aiwazian.messenger.utils.SharedFileCache
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val getShareTargetsUseCase: GetShareTargetsUseCase,
    private val sharedFileCache: SharedFileCache,
    private val messageSendQueue: MessageSendQueue
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ShareUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private var isInitialized = false
    
    fun init(sharedText: String, sharedFiles: List<Uri> = emptyList()) {
        if (isInitialized) return
        isInitialized = true
        
        _uiState.update { it.copy(sharedText = sharedText, sharedFiles = sharedFiles) }
        loadTargets()
    }
    
    private fun loadTargets() {
        viewModelScope.launch {
            val targets = getShareTargetsUseCase(_uiState.value.selectedChatIds)
            _uiState.update { it.copy(targets = targets) }
        }
    }
    
    fun toggleChatSelection(chatId: Long) {
        _uiState.update { state ->
            val selected = if (state.selectedChatIds.contains(chatId)) {
                state.selectedChatIds - chatId
            } else {
                state.selectedChatIds + chatId
            }
            
            state.copy(
                selectedChatIds = selected,
                targets = state.targets.map {
                    if (it.id == chatId) it.copy(isSelected = selected.contains(it.id)) else it
                }
            )
        }
    }
    
    /**
     * Отправка уходит в [MessageSendQueue], а не в viewModelScope: окно закроется
     * сразу, и вместе с ним умер бы каждый незаконченный запрос.
     */
    fun send() {
        val state = _uiState.value
        val text = state.sharedText
        val files = state.sharedFiles
        val chatIds = state.selectedChatIds
        
        if (state.isSending || chatIds.isEmpty()) return
        if (text.isBlank() && files.isEmpty()) return
        
        _uiState.update { it.copy(isSending = true) }
        
        viewModelScope.launch {
            val cachedFiles = if (files.isEmpty()) emptyList() else sharedFileCache.cache(files)
            
            if (files.isNotEmpty() && cachedFiles.isEmpty()) {
                _uiState.update { it.copy(isSending = false) }
                _uiEffect.emit(ShareUiEffect.ShowToast(UiText.StringResource(R.string.share_send_failed)))
                _uiEffect.emit(ShareUiEffect.Close)
                return@launch
            }
            
            chatIds.forEach { chatId ->
                if (cachedFiles.isEmpty()) {
                    messageSendQueue.enqueueText(chatId = chatId, text = text)
                } else {
                    // Подпись из системного шара уезжает вместе с файлами одним
                    // сообщением, а не отдельно.
                    messageSendQueue.enqueueFiles(
                        chatId = chatId,
                        uris = cachedFiles,
                        text = text.ifBlank { null }
                    )
                }
            }
            
            _uiState.update { it.copy(isSending = false) }
            _uiEffect.emit(ShareUiEffect.ShowToast(UiText.StringResource(R.string.share_sent)))
            _uiEffect.emit(ShareUiEffect.Close)
        }
    }
}
