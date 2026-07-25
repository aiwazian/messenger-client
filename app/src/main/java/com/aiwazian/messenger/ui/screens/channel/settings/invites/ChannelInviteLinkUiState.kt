/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.invites

import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.ui.components.ShareItem

data class ChannelInviteLinkUiState(
    val channelId: Long = -1,
    val activeLinks: List<InviteLink> = emptyList(),
    val inactiveLinks: List<InviteLink> = emptyList(),
    val linkIdToDelete: Long? = null,
    val showShareSheet: Boolean = false,
    val expandedMenuId: Long? = null,
    val availableChats: List<ShareItem> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val linkToShare: String? = null
)
