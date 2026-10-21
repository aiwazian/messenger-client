/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ChatViewModel.onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
    when (action) {
        FileAction.DOWNLOAD -> downloadFile(message, file)
        FileAction.PAUSE -> viewModelScope.launch { downloaderManager.pause(file.fileId) }
        FileAction.RESUME -> viewModelScope.launch { downloaderManager.resume(file.fileId) }
        FileAction.CANCEL -> viewModelScope.launch { downloaderManager.cancel(file.fileId) }
        FileAction.OPEN -> handleOpenFile(file)
        FileAction.PLAY -> {
            if (file.type == AttachmentType.VOICE) {
                playbackConnector.handlePlayVoice(file)
            } else if (file.extension.isAudioFile()) {
                playbackConnector.handlePlayMusic(file)
            }
        }
    }
}

private fun ChatViewModel.downloadFile(message: Message, file: MessageAttachment) {
    viewModelScope.launch {
        if (fileRepository.getById(file.fileId)?.path != null) return@launch

        chatRepository.getDownloadUrl(message.chatId, message.id, file.fileId)
            .onSuccess { url -> downloaderManager.download(url, file.name, file.fileId) }
    }
}

private fun ChatViewModel.handleOpenFile(file: MessageAttachment) {
    if (file.type == AttachmentType.IMAGE || file.type == AttachmentType.VIDEO || file.type == AttachmentType.GIF) {
        val index = _uiState.value.mediaItems.indexOfFirst { it.fileId == file.fileId }
        _uiState.update {
            it.copy(
                showFullScreenViewer = true,
                initialMediaIndex = index.coerceAtLeast(0)
            )
        }
    } else {
        viewModelScope.launch { fileHandler.openFile(file.localUri.toString()) }
    }
}

internal fun ChatViewModel.cancelUpload(tempMessageId: Long) {
    sendingJobs[tempMessageId]?.cancel()
    sendingJobs.remove(tempMessageId)
    viewModelScope.launch {
        val tempMessage = _uiState.value.chatItems.filterIsInstance<ChatItem.MessageItem>()
            .find { it.message.id == tempMessageId }?.message
        tempMessage?.attachments?.forEach { uploadManager.cancel(it.fileId) }
        chatRepository.deleteLocalMessage(tempMessageId)
    }
}

internal fun ChatViewModel.saveToGallery(uri: Uri) {
    if (!copyPolicy.canSaveMedia) return
    viewModelScope.launch {
        if (fileHandler.saveToGallery(uri.path ?: uri.toString())) {
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.successfully_saved_to_gallery)))
        } else {
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_to_gallery)))
            vibrationManager.vibrate(VibrationPattern.Error)
        }
    }
}

internal fun ChatViewModel.saveAttachmentsToDownloads(message: Message) {
    if (!copyPolicy.canSaveMedia) return
    viewModelScope.launch {
        val downloaded =
            message.attachments.filter { it.localUri != null && (it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.UPLOADED) }
        if (downloaded.isEmpty()) return@launch

        var successCount = 0
        downloaded.forEach {
            if (fileHandler.saveToDownloads(
                    it.localUri?.path ?: it.localUri.toString(),
                    it.name
                )
            ) successCount++
        }

        val res =
            if (successCount > 0) R.string.saved_to_downloads else R.string.failed_to_save_to_downloads
        _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(res)))
        if (successCount == 0) vibrationManager.vibrate(VibrationPattern.Error)
    }
}
