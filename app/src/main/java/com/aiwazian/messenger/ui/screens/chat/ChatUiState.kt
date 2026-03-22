/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.ui.screens.profile.Profile
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

data class ChatUiState(
    val chat: Chat = Chat(),
    val topBarTitle: String = "",
    val isSavedMessages: Boolean = false,
    val subTitle: String = "",
    val subscriberCount: Int? = null,
    val memberCount: Int? = null,
    val topBarActions: List<TopBarAction> = emptyList(),
    val chatItems: List<ChatItem> = emptyList(),
    val messageText: String = "",
    val profile: Profile? = null,
    val isConnected: Boolean = true,
    val isJoined: Boolean = true,
    val isMuted: Boolean = false,
    val showDeleteChatDialog: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val showDeleteMessageDialog: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val selectedMessages: Set<Message> = emptySet(),
    val userNamesCache: Map<Long, String> = emptyMap(),
    val currentUserId: Long = -1L,
    val isOwner: Boolean = false,
    val isLoading: Boolean = true
)