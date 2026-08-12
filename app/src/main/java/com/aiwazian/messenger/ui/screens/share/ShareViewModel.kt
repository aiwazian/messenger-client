/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.usecase.GetShareTargetsUseCase
import com.aiwazian.messenger.usecase.SendMessageUseCase
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
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ShareUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(sharedText: String) {
        if (_uiState.value.sharedText == sharedText) return
        
        _uiState.update { it.copy(sharedText = sharedText) }
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
    
    fun send() {
        val state = _uiState.value
        val text = state.sharedText
        val chatIds = state.selectedChatIds
        
        if (state.isSending || text.isBlank() || chatIds.isEmpty()) return
        
        _uiState.update { it.copy(isSending = true) }
        
        viewModelScope.launch {
            runCatching {
                chatIds.forEach { chatId ->
                    sendMessageUseCase(chatId, text)
                }
            }.onSuccess {
                _uiEffect.emit(ShareUiEffect.ShowToast(UiText.StringResource(R.string.share_sent)))
            }.onFailure {
                _uiEffect.emit(ShareUiEffect.ShowToast(UiText.StringResource(R.string.share_send_failed)))
            }
            
            _uiState.update { it.copy(isSending = false) }
            _uiEffect.emit(ShareUiEffect.Close)
        }
    }
}
