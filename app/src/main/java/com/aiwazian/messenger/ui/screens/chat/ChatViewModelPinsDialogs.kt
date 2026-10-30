/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ChatViewModel.onPinMessage(message: Message) {
    if (message.id <= 0 || message.messageType == MessageType.SYSTEM) return

    if (isSavedMessages) {
        pinMessageInternal(message, forEveryone = false)
        return
    }

    val state = _uiState.value
    val canManagePin =
        ChatType.fromId(state.chatId) == ChatType.PRIVATE || state.myPermissions.canPinMessages

    if (canManagePin) {
        val pinnedForEveryone = state.pinnedMessages
            .any { pin -> pin.messageId == message.id && pin.forEveryone }

        _uiState.update {
            it.copy(pinSheetMessage = message, pinForEveryone = pinnedForEveryone)
        }
    } else {
        pinMessageInternal(message, forEveryone = false)
    }
}

internal fun ChatViewModel.onUnpinMessage(message: Message) {
    if (message.id <= 0) return

    unpinMessageInternal(message, includeShared = isSavedMessages)
}

internal fun ChatViewModel.selectPinScope(forEveryone: Boolean) {
    _uiState.update { it.copy(pinForEveryone = forEveryone) }
}

internal fun ChatViewModel.dismissPinSheet() {
    _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }
}

internal fun ChatViewModel.confirmPin() {
    val state = _uiState.value
    val message = state.pinSheetMessage ?: return

    _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }

    pinMessageInternal(message, forEveryone = state.pinForEveryone)
}

internal fun ChatViewModel.confirmUnpin() {
    val state = _uiState.value
    val message = state.pinSheetMessage ?: return

    _uiState.update { it.copy(pinSheetMessage = null, pinForEveryone = false) }

    val includeShared =
        ChatType.fromId(state.chatId) == ChatType.PRIVATE || state.myPermissions.canPinMessages

    unpinMessageInternal(message, includeShared)
}

private fun ChatViewModel.pinMessageInternal(message: Message, forEveryone: Boolean) {
    viewModelScope.launch {
        pinnedMessagesRepository.pinMessage(_uiState.value.chatId, message.id, forEveryone)
            .onSuccess {
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_pinned))
                )
            }
            .onFailure {
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.pin_message_failed))
                )
            }
    }
}

private fun ChatViewModel.unpinMessageInternal(message: Message, includeShared: Boolean) {
    viewModelScope.launch {
        pinnedMessagesRepository.unpinMessage(_uiState.value.chatId, message.id, includeShared)
            .onSuccess {
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.message_unpinned))
                )
            }
            .onFailure {
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.unpin_message_failed))
                )
            }
    }
}

internal fun ChatViewModel.onLeaveClicked() {
    viewModelScope.launch {
        leaveChatUseCase(_uiState.value.chatId).onSuccess {
            hideLeaveDialog()
            _uiEffect.emit(ChatUiEffect.NavigateToMain)
        }
    }
}

internal fun ChatViewModel.onDeleteChatConfirmed() {
    viewModelScope.launch {
        if (chatRepository.deleteChat(
                _uiState.value.chatId,
                _uiState.value.deleteForRecipient
            )
        ) {
            hideDeleteChatDialog()
            _uiEffect.emit(ChatUiEffect.NavigateToMain)
        }
    }
}

internal fun ChatViewModel.onDeleteMessageConfirmed() {
    viewModelScope.launch {
        val deleteForRecipient = _uiState.value.deleteForRecipient
        _uiState.value.selectedMessages.forEach { message ->
            chatRepository.deleteMessage(_uiState.value.chatId, message.id, deleteForRecipient)
        }
        _uiState.update { it.copy(selectedMessages = emptySet()) }
        hideDeleteMessageDialog()
    }
}

internal fun ChatViewModel.showDeleteChatDialog() =
    _uiState.update { it.copy(showDeleteChatDialog = true, deleteForRecipient = false) }

internal fun ChatViewModel.hideDeleteChatDialog() =
    _uiState.update { it.copy(showDeleteChatDialog = false, deleteForRecipient = false) }

internal fun ChatViewModel.showDeleteMessageDialog() =
    _uiState.update { it.copy(showDeleteMessageDialog = true, deleteForRecipient = false) }

internal fun ChatViewModel.hideDeleteMessageDialog() =
    _uiState.update { it.copy(showDeleteMessageDialog = false, deleteForRecipient = false) }

internal fun ChatViewModel.showLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = true) }
internal fun ChatViewModel.hideLeaveDialog() = _uiState.update { it.copy(showLeaveDialog = false) }
internal fun ChatViewModel.setDeleteForRecipient(delete: Boolean) =
    _uiState.update { it.copy(deleteForRecipient = delete) }

internal fun ChatViewModel.showWallpaperSheet() = _uiState.update { it.copy(showWallpaperSheet = true) }
internal fun ChatViewModel.hideWallpaperSheet() = _uiState.update { it.copy(showWallpaperSheet = false) }

internal fun ChatViewModel.resetChatWallpaper() {
    viewModelScope.launch {
        wallpaperRepository.resetChatWallpaper(_uiState.value.chatId)
    }
}

internal fun ChatViewModel.dismissBlockDialog() {
    _uiState.update { it.copy(showBlockDialog = false) }
}

internal fun ChatViewModel.unblockUser() {
    viewModelScope.launch {
        userRepository.unblockUser(_uiState.value.chatId).onSuccess {
            dismissBlockDialog()
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.user_unblocked)))
        }.onFailure {
            _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.unexpected_error)))
            dismissBlockDialog()
        }
    }
}

internal fun ChatViewModel.onLinkClicked(url: String) {
    globalBottomSheetManager.handleInviteLink(
        url = url,
        currentChatId = _uiState.value.chatId,
        scope = viewModelScope,
        onNavigateToChat = { chatId -> viewModelScope.launch { _uiEffect.emit(ChatUiEffect.NavigateToChat(chatId)) } },
        onOpenUrl = { normalized -> viewModelScope.launch { _uiEffect.emit(ChatUiEffect.OpenUrl(normalized)) } },
        onShowSnackbar = { uiText -> viewModelScope.launch { _uiEffect.emit(ChatUiEffect.ShowSnackbar(uiText)) } }
    )
}

internal fun ChatViewModel.onUsernameClicked(username: String) {
    viewModelScope.launch {
        searchRepository.resolveUsername(username.removePrefix("@")).onSuccess { result ->
            if (result == null) {
                _uiEffect.emit(ChatUiEffect.ShowSnackbar(UiText.StringResource(R.string.chat_not_found)))
                vibrationManager.vibrate(VibrationPattern.Error)
            } else if (result.isBanned) {
                _uiState.update { s -> s.copy(showBannedDialog = true) }
                vibrationManager.vibrate(VibrationPattern.Error)
            } else {
                _uiEffect.emit(ChatUiEffect.NavigateToChat(result.chatId))
            }
        }
    }
}

internal fun ChatViewModel.onSubscribeViaInviteLink() {
    globalBottomSheetManager.subscribeViaInviteLink(
        scope = viewModelScope,
        onNavigateToChat = { chatId -> viewModelScope.launch { _uiEffect.emit(ChatUiEffect.NavigateToChat(chatId)) } },
        onShowSnackbar = { uiText -> viewModelScope.launch { _uiEffect.emit(ChatUiEffect.ShowSnackbar(uiText)) } }
    )
}

internal fun ChatViewModel.dismissInviteBottomSheet() {
    globalBottomSheetManager.dismissInviteBottomSheet()
}

internal fun ChatViewModel.dismissBannedDialog() {
    globalBottomSheetManager.hideBannedDialog()
}
