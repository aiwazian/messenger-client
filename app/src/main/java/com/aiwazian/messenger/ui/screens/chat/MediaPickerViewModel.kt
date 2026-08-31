/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.repository.DeviceMediaRepository
import com.aiwazian.messenger.utils.MessageSendQueue
import com.aiwazian.messenger.utils.media.VideoMetadata
import com.aiwazian.messenger.utils.media.VideoMetadataReader
import com.aiwazian.messenger.utils.media.VideoQuality
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
    val isLoading: Boolean = false,
    /**
     * Ступень сжатия, выбранная для видео. Чего здесь нет, то уйдёт со ступенью
     * по умолчанию: настройку открывать необязательно.
     */
    val videoQualities: Map<Uri, VideoQuality> = emptyMap(),
    /** Размеры видео, открытого во весь экран: по ним считаются ступени и вес. */
    val openedVideo: VideoMetadata? = null
)

/**
 * Галерея в шторке вложений: лента устройства, нумерованный выбор и отправка.
 *
 * Отправка идёт через [MessageSendQueue], а не через viewModelScope: шторка
 * закрывается сразу после нажатия, и своя корутина не дожила бы до конца
 * загрузки файлов.
 *
 * Подпись здесь больше не хранится — это черновик чата из ChatViewModel. Своя
 * подпись означала бы два разных текста: набранный в поле ввода пропадал бы
 * при открытии шторки, а набранный в шторке — при её закрытии.
 *
 * Миниатюр здесь тоже нет: кадры для сетки рисует Coil прямо в ячейке.
 */
@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val deviceMediaRepository: DeviceMediaRepository,
    private val videoMetadataReader: VideoMetadataReader,
    private val messageSendQueue: MessageSendQueue
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState = _uiState.asStateFlow()
    
    /** Чьи размеры сейчас ждём: пока читали, могли пролистать на другое видео. */
    private var openedVideoUri: Uri? = null
    
    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val media = deviceMediaRepository.getMedia()
            
            _uiState.update { it.copy(media = media, isLoading = false) }
        }
    }
    
    /** Шторка каждый раз открывается с чистым выбором. */
    fun reset() {
        openedVideoUri = null
        
        _uiState.update {
            it.copy(selected = emptyList(), videoQualities = emptyMap(), openedVideo = null)
        }
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
    
    /**
     * Медиа открыли во весь экран: у видео заодно читаются его размеры.
     *
     * Читать их сразу на всю ленту незачем: настройка сжатия открыта для одного
     * видео, а каждые размеры — это отдельное открытие файла.
     */
    fun openMedia(item: DeviceMediaItem?) {
        val uri = item?.takeIf { it.isVideo }?.uri
        
        openedVideoUri = uri
        
        /* Размеры прошлого видео здесь чужие, поэтому подпись пустеет сразу. */
        _uiState.update { it.copy(openedVideo = null) }
        
        if (uri == null) {
            return
        }
        
        viewModelScope.launch {
            val metadata = videoMetadataReader.read(uri)
            
            if (openedVideoUri == uri) {
                _uiState.update { it.copy(openedVideo = metadata) }
            }
        }
    }
    
    fun setVideoQuality(uri: Uri, quality: VideoQuality) {
        _uiState.update { it.copy(videoQualities = it.videoQualities + (uri to quality)) }
    }
    
    fun send(chatId: Long, replyTo: MessageReplyPreview?, caption: String) {
        val selected = _uiState.value.selected
        
        if (selected.isEmpty()) {
            return
        }
        
        sendUris(chatId = chatId, uris = selected, caption = caption, replyTo = replyTo)
        
        _uiState.update { it.copy(selected = emptyList(), videoQualities = emptyMap()) }
    }
    
    /**
     * Файлы из системного выбора уходят тем же путём, что и галерея.
     *
     * Раньше их отправлял ChatViewModel, и подпись к ним прикрепить было нечем:
     * текст оставался в поле ввода и уходил отдельным сообщением.
     */
    fun sendUris(chatId: Long, uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?) {
        if (uris.isEmpty()) {
            return
        }
        
        messageSendQueue.enqueueFiles(
            chatId = chatId,
            uris = uris,
            text = caption.trim().ifBlank { null },
            replyTo = replyTo,
            videoQualities = _uiState.value.videoQualities
        )
    }
}
