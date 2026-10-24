/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ForwardSourceAccess
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ChatViewModel.startForward(message: Message) {
    if (!copyPolicy.canForward()) return
    if (message.attachments.any { it.viewOnce }) return
    if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

    viewModelScope.launch {
        val candidates = loadShareCandidates()

        _uiState.update {
            it.copy(
                sharingLink = null,
                forwardingMessage = message,
                forwardCandidates = candidates,
                selectedForwardChatIds = emptySet(),
                isForwarding = false,
                forwardHideAuthor = false,
                forwardHideCaption = false,
                isForwardSheetVisible = true
            )
        }
    }
}

internal fun ChatViewModel.startShareLink(link: String) {
    if (link.isBlank()) return

    viewModelScope.launch {
        val candidates = loadShareCandidates()

        _uiState.update {
            it.copy(
                sharingLink = link,
                forwardingMessage = null,
                forwardCandidates = candidates,
                selectedForwardChatIds = emptySet(),
                isForwarding = false,
                forwardHideAuthor = false,
                forwardHideCaption = false,
                isForwardSheetVisible = true
            )
        }
    }
}

internal fun ChatViewModel.copyLink(link: String) {
    if (link.isBlank()) return

    clipboardService.copy(link)

    viewModelScope.launch {
        _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.copied)))
    }
}

private suspend fun ChatViewModel.loadShareCandidates() =
    chatRepository.getAllChats().firstOrNull().orEmpty().filter { chat ->
        when (ChatType.fromId(chat.id)) {
            ChatType.CHANNEL ->
                channelRepository.getByIdOrNull(chat.id)
                    .firstOrNull()?.ownerId == _uiState.value.myId

            ChatType.UNKNOWN -> false
            else -> true
        }
    }

internal fun ChatViewModel.toggleForwardTarget(chatId: Long) {
    _uiState.update { state ->
        val selected = state.selectedForwardChatIds
        state.copy(
            selectedForwardChatIds = if (chatId in selected) selected - chatId
            else selected + chatId
        )
    }
}

internal fun ChatViewModel.dismissForwardSheet() {
    _uiState.update {
        it.copy(
            isForwardSheetVisible = false,
            forwardingMessage = null,
            forwardCandidates = emptyList(),
            selectedForwardChatIds = emptySet(),
            isForwarding = false,
            forwardHideAuthor = false,
            forwardHideCaption = false,
            sharingLink = null
        )
    }
}

internal fun ChatViewModel.toggleForwardHideAuthor() {
    _uiState.update { it.copy(forwardHideAuthor = !it.forwardHideAuthor) }
}

internal fun ChatViewModel.toggleForwardHideCaption() {
    _uiState.update { state ->
        val hideCaption = !state.forwardHideCaption
        state.copy(
            forwardHideCaption = hideCaption,
            forwardHideAuthor = if (hideCaption) true else state.forwardHideAuthor
        )
    }
}

internal fun ChatViewModel.confirmForward() {
    val link = _uiState.value.sharingLink

    if (link != null) {
        confirmShareLink(link)
        return
    }

    val state = _uiState.value
    val message = state.forwardingMessage ?: return
    if (!copyPolicy.canForward()) return
    val targets = state.selectedForwardChatIds.toList()
    if (targets.isEmpty() || state.isForwarding) return

    _uiState.update { it.copy(isForwarding = true) }

    viewModelScope.launch {
        chatRepository.forwardMessage(
            sourceChatId = state.chatId,
            messageId = message.id,
            targetChatIds = targets,
            hideAuthor = state.forwardHideAuthor,
            hideCaption = state.forwardHideCaption
        )
            .onSuccess {
                dismissForwardSheet()
                if (targets.contains(state.chatId)) {
                    if (!_uiState.value.isAtLiveEdge) jumpToLatestInternal()
                    requestScrollTo(messageId = null, highlight = false, animate = true)
                }
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_forwarded))
                )
            }
            .onFailure { error ->
                Log.e("ChatViewModel", "Error forwarding message", error)
                _uiState.update { it.copy(isForwarding = false) }
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.forward_failed))
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
    }
}

private fun ChatViewModel.confirmShareLink(link: String) {
    val state = _uiState.value
    val targets = state.selectedForwardChatIds.toList()
    if (targets.isEmpty() || state.isForwarding) return

    _uiState.update { it.copy(isForwarding = true) }

    viewModelScope.launch {
        targets.forEachIndexed { index, targetChatId ->
            launch {
                sendMessageUseCase(
                    chatId = targetChatId,
                    message = link,
                    tempId = -System.currentTimeMillis() - index
                )
            }
        }

        dismissForwardSheet()

        if (targets.contains(state.chatId)) {
            if (!_uiState.value.isAtLiveEdge) jumpToLatestInternal()
            requestScrollTo(messageId = null, highlight = false, animate = true)
        }

        _uiEffect.emit(
            ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.share_sent))
        )
    }
}

internal fun ChatViewModel.onForwardedFromClicked(message: Message) {
    val forwardedFrom = message.forwardedFrom ?: return
    if (forwardedFrom.access != ForwardSourceAccess.OPEN) return
    if (forwardedFrom.chatId == _uiState.value.chatId) return

    viewModelScope.launch {
        _uiEffect.emit(ChatUiEffect.NavigateToChat(chatId = forwardedFrom.chatId))
    }
}
