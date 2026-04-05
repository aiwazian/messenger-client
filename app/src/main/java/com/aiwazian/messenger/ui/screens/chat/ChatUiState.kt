/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.profile.Profile

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
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val inviteLinkInfo: InviteLinkInfo? = null,
    val inviteLinkCode: String? = null,
    val showInviteBottomSheet: Boolean = false,
    val isProcessingInvite: Boolean = false,
    val inviteLinkError: String? = null,
    val showBannedDialog: Boolean = false
)