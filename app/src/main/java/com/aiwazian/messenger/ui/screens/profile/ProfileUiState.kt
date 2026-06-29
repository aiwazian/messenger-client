/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.UiText

data class ProfileChannelInfo(
    val id: Long,
    val name: String,
    val subscribers: Int,
    val avatarUri: Uri? = null,
    val lastMessage: Message? = null
)

data class ProfileUiState(
    val id: Long = -1,
    val profile: Profile? = null,
    val title: UiText = UiText.DynamicString(""),
    val subTitle: UiText = UiText.DynamicString(""),
    val avatars: List<Uri?> = emptyList(),
    val actions: List<TopBarAction> = emptyList(),
    val showLeaveDialog: Boolean = false,
    val myId: Long = -1,
    val inviteLinkInfo: InviteLinkInfo? = null,
    val inviteLinkCode: String? = null,
    val showInviteBottomSheet: Boolean = false,
    val isProcessingInvite: Boolean = false,
    val showBannedDialog: Boolean = false,
    val profileChannelInfo: ProfileChannelInfo? = null,
    val showBlockDialog: Boolean = false,
    val isBlockedStateForDialog: Boolean = false
)
