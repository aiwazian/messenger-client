/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
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
    val isMuted: Boolean = false,
    val showDeleteChatDialog: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val showDeleteMessageDialog: Boolean = false,
    val deleteForRecipient: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val selectedMessages: Set<Message> = emptySet(),
    val userNamesCache: Map<Long, String> = emptyMap(),
    val myId: Long = -1L,
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
    val showBlockDialog: Boolean = false
)
