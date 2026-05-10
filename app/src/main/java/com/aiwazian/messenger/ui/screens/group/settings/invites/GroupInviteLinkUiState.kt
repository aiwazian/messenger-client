/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.InviteLink

data class GroupInviteLinkUiState(
    val groupId: Long = -1,
    val activeLinks: List<InviteLink> = emptyList(),
    val inactiveLinks: List<InviteLink> = emptyList(),
    val selectedLinks: List<InviteLink> = emptyList(),
    val linkIdToDelete: Long? = null,
    val showShareSheet: Boolean = false,
    val expandedMenuId: Long? = null,
    val availableChats: List<Chat> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val linkToShare: String? = null
)