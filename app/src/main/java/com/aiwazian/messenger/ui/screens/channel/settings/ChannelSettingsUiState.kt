/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import android.net.Uri
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.enums.ChannelType

data class ChannelSettingsUiState(
    val channel: Channel = Channel(
        id = 0,
        name = "",
        bio = null,
        channelType = ChannelType.PRIVATE,
        username = null,
        subscribers = 0,
        removedUsers = null,
        isSubscribed = false,
        ownerId = null
    ),
    val originalChannelData: Channel? = null,
    val hasChanges: Boolean = false,
    val pendingAvatarUri: Uri? = null,
    val permissions: ChatAdminPermissions = ChatAdminPermissions(),
    val adminsCount: Int = 0,
    val joinRequestsCount: Int = 0
) {
    
    val isOwner: Boolean
        get() = permissions.isOwner
    
    val canEditProfile: Boolean
        get() = permissions.isOwner || permissions.canEditProfile
    
    val canManageInviteLinks: Boolean
        get() = permissions.isOwner || permissions.canManageInviteLinks
    
    val canManageAdmins: Boolean
        get() = permissions.isOwner || permissions.canManageAdmins
}
