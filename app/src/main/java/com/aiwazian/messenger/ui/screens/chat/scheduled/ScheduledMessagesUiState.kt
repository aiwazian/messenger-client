/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.scheduled

import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.screens.chat.ChatCopyPolicy
import com.aiwazian.messenger.ui.screens.chat.ChatItem

data class ScheduledMessagesUiState(
    val chatId: Long = -1L,
    val myId: Long = -1L,
    val chatType: ChatType = ChatType.UNKNOWN,
    val items: List<ChatItem> = emptyList(),
    val isLoading: Boolean = true,
    val userNamesCache: Map<Long, String> = emptyMap(),
    val memberTagsCache: Map<Long, String> = emptyMap(),
    val audioMetadata: Map<String, AudioTrackMetadata> = emptyMap(),
    val currentPlayingVoiceFileId: String? = null,
    val isVoicePlaying: Boolean = false,
    val voicePositionMs: Int = 0,
    val voiceDurationMs: Int = 0,
    val currentMusicFileId: String? = null,
    val isMusicPlaying: Boolean = false,
    val musicPositionMs: Int = 0,
    val musicDurationMs: Int = 0,
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f,
    val showFullScreenViewer: Boolean = false,
    val rescheduleMessage: Message? = null,
    val deleteMessage: Message? = null,
    val showScheduleSendSheet: Boolean = false
) {
    val copyPolicy: ChatCopyPolicy
        get() = ChatCopyPolicy.Unrestricted
}
