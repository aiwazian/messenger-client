/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun ChatViewModel.startReply(message: Message) {
    if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

    if (_uiState.value.editingMessageId != null) cancelEditing()

    val state = _uiState.value
    val chatType = ChatType.fromId(state.chatId)

    val senderName = when {
        message.senderId == state.myId -> state.myName
        else -> state.userNamesCache[message.senderId]
            ?: state.chatName.asString(context)
    }

    val preview = MessageReplyPreview(
        messageId = message.id,
        chatId = state.chatId,
        senderId = message.senderId,
        senderName = senderName,
        chatName = if (chatType == ChatType.PRIVATE) null
        else state.chatName.asString(context),
        text = message.text,
        attachmentTypes = message.attachments.map { it.type },
        attachmentExtensions = message.attachments.map { it.extension },
        messageType = message.messageType,
        stickerEmoji = message.sticker?.emojis?.firstOrNull()
    )

    if (chatType != ChatType.PRIVATE) loadUserName(message.senderId)

    _uiState.update { it.copy(replyToMessage = preview) }
    replyDraftCache.save(state.myId, state.chatId, preview)
}

internal fun ChatViewModel.onReplyPreviewClicked(message: Message) {
    val preview = message.replyTo ?: return

    if (isInCurrentChat(preview)) {
        jumpToMessage(preview.messageId, returnToMessageId = message.id)
    } else {
        val targetChatId = preview.chatId ?: return
        viewModelScope.launch {
            _uiEffect.emit(
                ChatUiEffect.NavigateToChat(
                    chatId = targetChatId,
                    scrollToMessageId = preview.messageId
                )
            )
        }
    }
}

internal fun ChatViewModel.clearReply() {
    replyDraftCache.clear(_uiState.value.myId, _uiState.value.chatId)
    if (_uiState.value.replyToMessage == null) return
    _uiState.update { it.copy(replyToMessage = null) }
}

private fun ChatViewModel.isInCurrentChat(preview: MessageReplyPreview): Boolean {
    val state = _uiState.value
    val originChatId = preview.chatId ?: return true
    if (originChatId == state.chatId) return true
    return ChatType.fromId(state.chatId) == ChatType.PRIVATE && originChatId == state.myId
}

internal fun ChatViewModel.onSendMessageInternal(
    text: String,
    replyTo: MessageReplyPreview? = null,
    silent: Boolean = false
) {
    val tempId = -System.currentTimeMillis()
    val job = viewModelScope.launch {
        try {
            if (!_uiState.value.isAtLiveEdge) jumpToLatestInternal()
            sendMessageUseCase(
                _uiState.value.chatId,
                message = text,
                tempId = tempId,
                replyTo = replyTo,
                silent = silent
            )
            requestScrollTo(messageId = null, highlight = false, animate = true)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error sending message", e)
        } finally {
            sendingJobs.remove(tempId)
        }
    }
    sendingJobs[tempId] = job
}

internal suspend fun ChatViewModel.handleEditMessage(editingId: Long) {
    val newText = _uiState.value.messageText.trim()
    val originalText = _uiState.value.editingOriginalText
    if (newText.isEmpty() || newText == originalText) {
        cancelEditing()
        return
    }
    cancelEditing()
    chatRepository.editMessage(_uiState.value.chatId, editingId, newText)
        .onSuccess { editedMessage ->
            chatRepository.updateLocalMessage(
                editingId,
                editedMessage.text,
                editedMessage.editedAt
            )
        }
}

internal fun ChatViewModel.retrySendMessage(message: Message) {
    viewModelScope.launch {
        if (message.attachments.isNotEmpty()) return@launch

        message.text?.let { text ->
            chatRepository.deleteLocalMessage(message.id)
            onSendMessageInternal(text, message.replyTo, silent = message.silent)
        }
    }
}

internal fun ChatViewModel.cancelSendMessage(message: Message) {
    sendingJobs[message.id]?.cancel()
    sendingJobs.remove(message.id)
    viewModelScope.launch {
        chatRepository.deleteLocalMessage(message.id)
    }
}

internal fun ChatViewModel.startEditing(message: Message) {
    if (message.forwardedFrom != null) return

    val now = System.currentTimeMillis()
    if (!isSavedMessages && now - message.sendTime > EDIT_WINDOW_MS) {
        viewModelScope.launch {
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.edit_time_limit_error)))
        }
        return
    }
    if (message.text.isNullOrBlank()) return

    clearReply()

    _uiState.update {
        it.copy(
            editingMessageId = message.id,
            editingOriginalText = message.text,
            messageText = message.text
        )
    }
}

private const val EDIT_WINDOW_MS = 24 * 60 * 60 * 1000L
