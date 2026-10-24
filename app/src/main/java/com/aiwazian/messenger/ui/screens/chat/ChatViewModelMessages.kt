/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.screens.chat.paging.MessageWindowPager
import com.aiwazian.messenger.ui.screens.chat.paging.ScrollTarget
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ChatViewModel.observeMessages() {
    _uiState.update { it.copy(isLoading = true) }
    val chatId = _uiState.value.chatId

    windowObserverJob?.cancel()
    windowObserverJob = viewModelScope.launch {
        val userId = userRepository.getMe().first().id

        val pager = MessageWindowPager(
            chatRepository = chatRepository,
            userId = userId,
            chatId = chatId,
            pageSize = ChatViewModel.PAGE_SIZE,
            aroundRadius = ChatViewModel.AROUND_RADIUS,
            maxWindowMessages = ChatViewModel.MAX_WINDOW_MESSAGES
        )
        messagePager = pager

        launch {
            pager.state.collect { window ->
                _uiState.update {
                    it.copy(
                        isLoadingOlder = window.isLoadingBefore,
                        isLoadingNewer = window.isLoadingAfter,
                        isLoadingMore = window.isLoadingBefore || window.isLoadingAfter,
                        hasMoreMessages = window.hasMoreBefore,
                        hasMoreNewerMessages = window.hasMoreAfter,
                        isRelocating = window.isRelocating,
                        isAtLiveEdge = window.isAtLive
                    )
                }
            }
        }

        launch {
            pager.state
                .map { it.bounds }
                .distinctUntilChanged()
                .flatMapLatest { bounds ->
                    chatRepository.getMessagesWindowFlow(userId, chatId, bounds)
                }
                .collect { messages ->
                    updateChatItems(messages)
                    _uiState.update { it.copy(isLoading = false) }

                    if (!isFirstLoadDone && pager.state.value.isInitialized) {
                        isFirstLoadDone = true
                        _uiState.update { it.copy(isFirstLoadDone = true) }
                        val hasUnreadTarget = _uiState.value.firstUnreadMessageId != null
                        if (messages.isNotEmpty() && !hasUnreadTarget) {
                            requestScrollTo(
                                messageId = null,
                                highlight = false,
                                animate = false
                            )
                        }
                    }

                    handleNewestMessage(messages, pager.state.value.isAtLive)
                }
        }

        val firstUnreadId = pager.openAtFirstUnread()
        _uiState.update { it.copy(firstUnreadMessageId = firstUnreadId) }
        if (firstUnreadId != null) {
            unreadAnchorMessageId = firstUnreadId
            updateChatItems(lastMessages)

            requestScrollTo(
                messageId = firstUnreadId,
                highlight = false,
                animate = false,
                viewportFraction = ChatViewModel.UNREAD_VIEWPORT_FRACTION
            )
        }
        if (!isFirstLoadDone) {
            isFirstLoadDone = true
            _uiState.update { it.copy(isFirstLoadDone = true, isLoading = false) }
        }
    }
}

internal fun ChatViewModel.observeRealtimeEvents() {
    viewModelScope.launch {
        realtimeEventSyncService.groupReadEvents.collect { payload ->
            if (payload.chatId == _uiState.value.chatId && payload.userId != _uiState.value.myId) {
                val readerInfo =
                    MessageReadInfo(
                        userId = payload.userId,
                        firstName = "",
                        lastName = null,
                        readAt = payload.time
                    )
                val current = _uiState.value.groupReadInfo
                val existing = current[payload.messageId].orEmpty()
                if (existing.none { it.userId == payload.userId }) {
                    _uiState.update { it.copy(groupReadInfo = current + (payload.messageId to (existing + readerInfo))) }
                }
                loadUserName(payload.userId)
            }
        }
    }

    viewModelScope.launch {
        realtimeEventSyncService.chatRemovedEvents.collect { removedChatId ->
            if (removedChatId == _uiState.value.chatId) _uiEffect.emit(ChatUiEffect.NavigateToMain)
        }
    }
}

internal fun ChatViewModel.observePinnedMessages() {
    val chatId = _uiState.value.chatId

    viewModelScope.launch {
        pinnedMessagesRepository.refreshPinnedMessages(chatId)
    }

    viewModelScope.launch {
        pinnedMessagesRepository.observePinnedMessages(chatId).collect { pins ->
            _uiState.update { it.copy(pinnedMessages = pins) }
            updateChatItems(lastMessages)
        }
    }
}

internal fun ChatViewModel.updateChatItems(messages: List<Message>) {
    lastMessages = messages
    val state = _uiState.value
    val mapper = ChatItemMapper(
        context = context,
        myId = state.myId,
        chatId = state.chatId,
        isOwner = state.isOwner,
        isJoined = state.isJoined,
        userNamesCache = state.userNamesCache,
        memberTagsCache = state.memberTagsCache,
        groupReadInfo = state.groupReadInfo,
        highlightedMessageId = state.highlightedMessageId,
        unreadAnchorMessageId = unreadAnchorMessageId,
        copyPolicy = copyPolicy,
        pinnedByMeMessageIds = state.pinnedMessages
            .filter { pin -> !pin.forEveryone }
            .map { pin -> pin.messageId }
            .toSet(),
        sharedPinnedMessageIds = state.pinnedMessages
            .filter { pin -> pin.forEveryone }
            .map { pin -> pin.messageId }
            .toSet(),
        canPinForEveryone = state.myPermissions.canPinMessages,
        onCopyText = { copyToClipboard(it) },
        onCopyContactPhone = { copyContactPhone(it) },
        onCallContact = { onPhoneClicked(it) },
        onEditMessage = { startEditing(it) },
        onDeleteMessage = {
            showDeleteMessageDialog()
            selectMessage(it)
        },
        onRetrySendMessage = { retrySendMessage(it) },
        onCancelSendMessage = { cancelSendMessage(it) },
        onReplyMessage = { startReply(it) },
        onForwardMessage = { startForward(it) },
        onPinMessage = { onPinMessage(it) },
        onUnpinMessage = { onUnpinMessage(it) },
        onLoadUserName = { loadUserName(it) }
    )

    val chatItems = mapper.map(messages).asReversed()
    val newMediaItems = messages.flatMap { it.attachments }
        .filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.GIF }

    _uiState.update { it.copy(chatItems = chatItems, mediaItems = newMediaItems) }

    if (autoDownloadMedia) {
        messages.forEach { msg ->
            msg.attachments.forEach { attachment ->
                val alreadyLocal = attachment.localUri != null
                if (!alreadyLocal &&
                    (attachment.status == DownloadStatus.IDLE || attachment.status == DownloadStatus.UPLOADED)
                ) {
                    val shouldDownload = when (attachment.type) {
                        AttachmentType.VOICE -> true
                        AttachmentType.IMAGE, AttachmentType.GIF -> autoDownloadPhotos
                        AttachmentType.VIDEO -> autoDownloadVideos && attachment.size <= 10 * 1024 * 1024
                        AttachmentType.FILE -> autoDownloadFiles && attachment.size <= 10 * 1024 * 1024
                    }
                    if (shouldDownload) onFileAction(msg, attachment, FileAction.DOWNLOAD)
                }
            }
        }
    }
}

internal fun ChatViewModel.loadOlderMessages() {
    val pager = messagePager ?: return
    viewModelScope.launch { pager.loadBefore() }
}

internal fun ChatViewModel.loadNewerMessages() {
    val pager = messagePager ?: return
    viewModelScope.launch { pager.loadAfter() }
}

internal fun ChatViewModel.jumpToMessage(messageId: Long, returnToMessageId: Long? = null) {
    val pager = messagePager ?: return
    viewModelScope.launch {
        if (returnToMessageId != null) {
            returnStack.addLast(returnToMessageId)
            _uiState.update { it.copy(canJumpBack = true) }
        }

        if (!pager.containsMessage(messageId)) {
            val loaded = pager.jumpTo(messageId)
            if (!loaded) {
                _uiEffect.emit(
                    ChatUiEffect.ShowSnackbar(
                        UiText.StringResource(R.string.jump_to_message_failed)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
        }

        requestScrollTo(messageId = messageId, highlight = true, animate = false)
    }
}

internal fun ChatViewModel.jumpToMessageWhenReady(messageId: Long) {
    viewModelScope.launch {
        var attempts = 0
        while (messagePager == null && attempts < 50) {
            delay(100.milliseconds)
            attempts++
        }
        if (messagePager == null) return@launch
        jumpToMessage(messageId)
    }
}

internal fun ChatViewModel.jumpToLatest() {
    viewModelScope.launch { jumpToLatestInternal() }
}

internal suspend fun ChatViewModel.jumpToLatestInternal() {
    val pager = messagePager ?: return
    val wasAtLive = pager.state.value.isAtLive
    if (!wasAtLive) pager.openAtLatest()
    returnStack.clear()
    _uiState.update { it.copy(canJumpBack = false) }
    requestScrollTo(messageId = null, highlight = false, animate = wasAtLive)

    _uiState.update { it.copy(unreadCount = 0, firstUnreadMessageId = null) }
    chatRepository.markAllAsRead(_uiState.value.chatId)
}

internal fun ChatViewModel.requestScrollTo(
    messageId: Long?,
    highlight: Boolean,
    animate: Boolean,
    viewportFraction: Float = 1f / 3f
) {
    _uiState.update {
        it.copy(
            scrollTarget = ScrollTarget(
                messageId = messageId,
                highlight = highlight,
                animate = animate,
                viewportFraction = viewportFraction
            )
        )
    }
}

internal fun ChatViewModel.onScrollTargetHandled(requestId: Long) {
    val target = _uiState.value.scrollTarget ?: return
    if (target.requestId != requestId) return
    _uiState.update { it.copy(scrollTarget = null) }
    val messageId = target.messageId
    if (target.highlight && messageId != null) highlightMessage(messageId)
}

private fun ChatViewModel.highlightMessage(messageId: Long) {
    highlightJob?.cancel()
    highlightJob = viewModelScope.launch {
        _uiState.update { it.copy(highlightedMessageId = messageId) }
        updateChatItems(lastMessages)
        delay(2000.milliseconds)
        _uiState.update { it.copy(highlightedMessageId = null) }
        updateChatItems(lastMessages)
    }
}

internal fun ChatViewModel.onMessagesSeen(messageId: Long) {
    if (messageId <= reportedReadUpToId) return
    reportedReadUpToId = messageId
    viewModelScope.launch {
        chatRepository.markReadUpTo(_uiState.value.chatId, messageId)
    }
}

internal fun ChatViewModel.onViewportAtBottomChanged(atBottom: Boolean) {
    isViewportAtBottom = atBottom
}

private fun ChatViewModel.handleNewestMessage(messages: List<Message>, isAtLive: Boolean) {
    val newest = messages.lastOrNull() ?: return
    val newestId = newest.id
    if (newestId <= lastKnownNewestId) return

    val isFirstFill = lastKnownNewestId == 0L
    lastKnownNewestId = newestId
    if (isFirstFill) return

    val isMine = newest.senderId == _uiState.value.myId
    if (isMine || (isViewportAtBottom && isAtLive)) {
        requestScrollTo(messageId = null, highlight = false, animate = true)
    }
}

internal fun ChatViewModel.loadUserName(userId: Long) {
    if (_uiState.value.userNamesCache.containsKey(userId)) return
    viewModelScope.launch {
        try {
            userRepository.getById(userId).collect { user ->
                val name = "${user.firstName} ${user.lastName.orEmpty()}".trim()
                _uiState.update { it.copy(userNamesCache = it.userNamesCache + (userId to name)) }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error loading user name", e)
        }
    }
}
