/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatContactItem
import com.aiwazian.messenger.domain.ChatMediaCounts
import com.aiwazian.messenger.domain.ChatMediaItem

data class ChatMediaUiState(
    val media: List<ChatMediaItem> = emptyList(),
    val files: List<ChatMediaItem> = emptyList(),
    val music: List<ChatMediaItem> = emptyList(),
    val voices: List<ChatMediaItem> = emptyList(),
    val contacts: List<ChatContactItem> = emptyList(),
    val isMediaLoading: Boolean = true,
    val isFilesLoading: Boolean = true,
    val isMusicLoading: Boolean = true,
    val isVoicesLoading: Boolean = true,
    val isContactsLoading: Boolean = true,
    val selectedAttachmentIds: Set<Int> = emptySet(),
    val selectedContactMessageIds: Set<Long> = emptySet(),
    val canForward: Boolean = false,
    val isForwardSheetVisible: Boolean = false,
    val forwardCandidates: List<Chat> = emptyList(),
    val selectedForwardChatIds: Set<Long> = emptySet(),
    val isForwarding: Boolean = false,
    val forwardHideAuthor: Boolean = false,
    val forwardHideCaption: Boolean = false,
    val hasError: Boolean = false,
    val counts: ChatMediaCounts? = null,
    val myId: Long = 0,
    val musicMetadata: Map<String, AudioTrackMetadata> = emptyMap(),
    val playingFileId: String? = null,
    val isVoicePlaying: Boolean = false,
    val musicPlayingFileId: String? = null,
    val isMusicPlaying: Boolean = false,
    val musicPositionMs: Int = 0,
    val musicDurationMs: Int = 0,
    val initialMediaIndex: Int = 0,
    val showFullScreenViewer: Boolean = false,
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f
)

data class ShowInChatResult(
    val messageId: Long
)
