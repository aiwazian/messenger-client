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
import com.aiwazian.messenger.extensions.isPdfFile
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ChatViewModel.onFileAction(message: Message, file: MessageAttachment, action: FileAction) {
    interactions.onFileAction(message, file, action)
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
    interactions.saveToGallery(uri) { success ->
        viewModelScope.launch {
            if (success) {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.successfully_saved_to_gallery)))
            } else {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_to_gallery)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}

internal fun ChatViewModel.saveAttachmentsToDownloads(message: Message) {
    interactions.saveAttachmentsToDownloads(message)
}
