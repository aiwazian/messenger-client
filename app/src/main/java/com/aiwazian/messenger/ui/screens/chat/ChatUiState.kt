package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.paging.ScrollTarget
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.UiText

data class ChatUiState(
    val chatId: Long = -1,
    val chatName: UiText = UiText.DynamicString(""),
    val subTitle: UiText = UiText.DynamicString(""),
    val avatarUri: Uri? = null,
    val topBarActions: List<TopBarAction> = emptyList(),
    val chatItems: List<ChatItem> = emptyList(),
    val messageText: String = "",
    val isConnected: Boolean = true,
    val isJoined: Boolean = true,
    val isOwner: Boolean = false,
    val myPermissions: ChatAdminPermissions = ChatAdminPermissions(),
    val isMuted: Boolean = false,
    val showDeleteChatDialog: Boolean = false,
    val showDeleteMessageDialog: Boolean = false,
    val deleteForRecipient: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val selectedMessages: Set<Message> = emptySet(),
    val userNamesCache: Map<Long, String> = emptyMap(),
    val memberTagsCache: Map<Long, String> = emptyMap(),
    val myId: Long = -1L,
    val myName: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val inviteLinkInfo: InviteLinkInfo? = null,
    val inviteLinkCode: String? = null,
    val showInviteBottomSheet: Boolean = false,
    val isProcessingInvite: Boolean = false,
    val inviteLinkError: String? = null,
    val showBannedDialog: Boolean = false,
    val mediaItems: List<MessageAttachment> = emptyList(),
    val initialMediaIndex: Int = 0,
    val showFullScreenViewer: Boolean = false,
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f,
    val canDownloadMedia: Boolean = true,
    val noCopy: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingLocked: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val recordingAmplitude: Float = 0f,
    val showMicrophonePermissionSheet: Boolean = false,
    val currentPlayingVoiceFileId: String? = null,
    val isVoicePlaying: Boolean = false,
    val voicePositionMs: Int = 0,
    val voiceDurationMs: Int = 0,
    val isFirstLoadDone: Boolean = false,
    val groupReadInfo: Map<Long, List<MessageReadInfo>> = emptyMap(),
    val editingMessageId: Long? = null,
    val editingOriginalText: String? = null,
    val isBlocked: Boolean = false,
    val isBlockedByThem: Boolean = false,
    val showBlockDialog: Boolean = false,
    val keyboardHeight: Float = DataStoreManager.DEFAULT_KEYBOARD_HEIGHT,

    val replyToMessage: MessageReplyPreview? = null,

    val forwardingMessage: Message? = null,
    val isForwardSheetVisible: Boolean = false,
    val forwardCandidates: List<Chat> = emptyList(),
    val selectedForwardChatIds: Set<Long> = emptySet(),
    val isForwarding: Boolean = false,
    val sharingLink: String? = null,

    val isLoadingOlder: Boolean = false,
    val isLoadingNewer: Boolean = false,
    val hasMoreNewerMessages: Boolean = false,
    val isRelocating: Boolean = false,
    val isAtLiveEdge: Boolean = true,
    val scrollTarget: ScrollTarget? = null,
    val highlightedMessageId: Long? = null,
    val canJumpBack: Boolean = false,
    val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null,

    val isMessageSearchActive: Boolean = false,
    val messageSearchQuery: String = "",
    val messageSearchResults: List<MessageSearchHit> = emptyList(),
    val isSearchingMessages: Boolean = false,
    val hasMoreSearchResults: Boolean = false,
    val messageSearchTotal: Int = 0,
    val isMessageSearchTotalExact: Boolean = true,
    val messageSearchIndex: Int = -1,
    val isMessageSearchListMode: Boolean = false,
    val messageSearchSenders: Map<Long, MessageSearchSender> = emptyMap()
) {

    val copyPolicy: ChatCopyPolicy
        get() = ChatCopyPolicy(noCopy)

    val canGoToOlderSearchResult: Boolean
        get() = messageSearchIndex + 1 < messageSearchResults.size || hasMoreSearchResults

    val canGoToNewerSearchResult: Boolean
        get() = messageSearchIndex > 0
}

data class MessageSearchSender(
    val name: String,
    val avatarUri: Uri? = null
)
