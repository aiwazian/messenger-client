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
import com.aiwazian.messenger.utils.media.MediaTransform
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
    val selected: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val videoQualities: Map<Uri, VideoQuality> = emptyMap(),
    val mediaTransforms: Map<Uri, MediaTransform> = emptyMap(),
    val openedVideo: VideoMetadata? = null
)

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val deviceMediaRepository: DeviceMediaRepository,
    private val videoMetadataReader: VideoMetadataReader,
    private val messageSendQueue: MessageSendQueue
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState = _uiState.asStateFlow()
    
    private var openedVideoUri: Uri? = null
    
    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val media = deviceMediaRepository.getMedia()
            
            _uiState.update { it.copy(media = media, isLoading = false) }
        }
    }
    
    fun reset() {
        openedVideoUri = null
        
        _uiState.update {
            it.copy(
                selected = emptyList(),
                videoQualities = emptyMap(),
                mediaTransforms = emptyMap(),
                openedVideo = null
            )
        }
    }
    
    fun toggleSelection(uri: Uri) {
        _uiState.update { state ->
            val selected = if (state.selected.contains(uri)) {
                state.selected - uri
            } else {
                state.selected + uri
            }
            
            state.copy(selected = selected)
        }
    }
    
    fun openMedia(item: DeviceMediaItem?) {
        val uri = item?.takeIf { it.isVideo }?.uri
        
        openedVideoUri = uri
        
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
    
    fun setMediaTransform(uri: Uri, transform: MediaTransform) {
        _uiState.update { state ->
            val transforms = if (transform.isIdentity) {
                state.mediaTransforms - uri
            } else {
                state.mediaTransforms + (uri to transform)
            }
            
            state.copy(mediaTransforms = transforms)
        }
    }
    
    fun send(chatId: Long, replyTo: MessageReplyPreview?, caption: String) {
        val selected = _uiState.value.selected
        
        if (selected.isEmpty()) {
            return
        }
        
        sendUris(chatId = chatId, uris = selected, caption = caption, replyTo = replyTo)
        
        _uiState.update {
            it.copy(
                selected = emptyList(),
                videoQualities = emptyMap(),
                mediaTransforms = emptyMap()
            )
        }
    }
    
    fun sendUris(chatId: Long, uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?) {
        if (uris.isEmpty()) {
            return
        }
        
        messageSendQueue.enqueueFiles(
            chatId = chatId,
            uris = uris,
            text = caption.trim().ifBlank { null },
            replyTo = replyTo,
            videoQualities = _uiState.value.videoQualities,
            mediaTransforms = _uiState.value.mediaTransforms
        )
    }
}
