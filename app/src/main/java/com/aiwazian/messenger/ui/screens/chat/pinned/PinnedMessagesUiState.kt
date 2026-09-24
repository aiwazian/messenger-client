/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.pinned

import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.screens.chat.ChatCopyPolicy
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.utils.UiText

data class PinnedMessagesUiState(
    val isLoading: Boolean = true,
    val myId: Long = -1L,
    val chatType: ChatType = ChatType.UNKNOWN,
    val isOwner: Boolean = false,
    val isJoined: Boolean = true,
    val myPermissions: ChatAdminPermissions = ChatAdminPermissions(),
    val noCopy: Boolean = false,
    val peerNoCopy: Boolean = false,
    val userNamesCache: Map<Long, String> = emptyMap(),
    val memberTagsCache: Map<Long, String> = emptyMap(),
    val groupReadInfo: Map<Long, List<MessageReadInfo>> = emptyMap(),
    val forEveryoneItems: List<ChatItem> = emptyList(),
    val forMeItems: List<ChatItem> = emptyList(),
    val pinnedCount: Int = 0,
    val currentPlayingVoiceFileId: String? = null,
    val isVoicePlaying: Boolean = false,
    val voicePositionMs: Int = 0,
    val voiceDurationMs: Int = 0,
    val currentMusicFileId: String? = null,
    val isMusicPlaying: Boolean = false,
    val musicPositionMs: Int = 0,
    val musicDurationMs: Int = 0,
    val audioMetadata: Map<String, AudioTrackMetadata> = emptyMap(),
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f,
    val showFullScreenViewer: Boolean = false,
    val pinSheetMessage: Message? = null,
    val pinForEveryone: Boolean = false,
    val deleteMessage: Message? = null,
    val deleteForRecipient: Boolean = false,
    val forwardingMessage: Message? = null,
    val isForwardSheetVisible: Boolean = false,
    val forwardCandidates: List<Chat> = emptyList(),
    val selectedForwardChatIds: Set<Long> = emptySet(),
    val isForwarding: Boolean = false,
    val forwardHideAuthor: Boolean = false,
    val forwardHideCaption: Boolean = false
) {

    val copyPolicy: ChatCopyPolicy
        get() = ChatCopyPolicy(noCopy = noCopy, peerNoCopy = peerNoCopy)

    val canManagePin: Boolean
        get() = chatType == ChatType.PRIVATE || myPermissions.canPinMessages
}

sealed interface PinnedMessagesUiEffect {
    data object NavigateBack : PinnedMessagesUiEffect
    data class ShowSnackbar(val message: UiText) : PinnedMessagesUiEffect
    data class OpenUrl(val url: String) : PinnedMessagesUiEffect
    data class OpenEmail(val email: String) : PinnedMessagesUiEffect
    data class NavigateToChat(
        val chatId: Long,
        val scrollToMessageId: Long? = null
    ) : PinnedMessagesUiEffect

    data class OpenInChat(
        val message: Message,
        val action: PinnedMessageOpenAction
    ) : PinnedMessagesUiEffect
}

enum class PinnedMessageOpenAction {
    REPLY,
    EDIT
}

data class PinnedMessageResult(
    val message: Message,
    val action: PinnedMessageOpenAction
)
