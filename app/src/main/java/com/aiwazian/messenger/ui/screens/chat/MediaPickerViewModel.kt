/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.repository.DeviceMediaRepository
import com.aiwazian.messenger.utils.MessageSendQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaPickerUiState(
    val media: List<DeviceMediaItem> = emptyList(),
    /** Порядок важен: номер в кружке — это позиция в этом списке. */
    val selected: List<Uri> = emptyList(),
    val caption: String = "",
    val isLoading: Boolean = false
)

/**
 * Галерея в шторке вложений: лента устройства, нумерованный выбор и отправка.
 *
 * Отправка идёт через [MessageSendQueue], а не через viewModelScope: шторка
 * закрывается сразу после нажатия, и своя корутина не дожила бы до конца
 * загрузки файлов.
 */
@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val deviceMediaRepository: DeviceMediaRepository,
    private val messageSendQueue: MessageSendQueue
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState = _uiState.asStateFlow()
    
    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val media = deviceMediaRepository.getMedia()
            
            _uiState.update { it.copy(media = media, isLoading = false) }
        }
    }
    
    /** Шторка каждый раз открывается с чистым выбором. */
    fun reset() {
        _uiState.update { it.copy(selected = emptyList(), caption = "") }
    }
    
    fun toggleSelection(uri: Uri) {
        _uiState.update { state ->
            val selected = if (state.selected.contains(uri)) {
                // Убрали не последнее — номера остальных сдвигаются сами.
                state.selected - uri
            } else {
                state.selected + uri
            }
            
            state.copy(selected = selected)
        }
    }
    
    fun changeCaption(caption: String) {
        _uiState.update { it.copy(caption = caption) }
    }
    
    suspend fun thumbnail(uri: Uri): Bitmap? = deviceMediaRepository.getVideoThumbnail(uri)
    
    fun send(chatId: Long, replyTo: MessageReplyPreview?) {
        val state = _uiState.value
        
        if (state.selected.isEmpty()) {
            return
        }
        
        messageSendQueue.enqueueFiles(
            chatId = chatId,
            uris = state.selected,
            text = state.caption.trim().ifBlank { null },
            replyTo = replyTo
        )
        
        _uiState.update { it.copy(selected = emptyList(), caption = "") }
    }
}
