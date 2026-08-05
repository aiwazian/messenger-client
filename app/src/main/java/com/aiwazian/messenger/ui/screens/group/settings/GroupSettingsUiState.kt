/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import android.net.Uri
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.enums.GroupType

/**
 * Состояние экрана настроек группы.
 *
 * Права приходят с сервера и решают, какие блоки вообще попадают в композицию.
 */
data class GroupSettingsUiState(
    val group: Group = Group(
        id = 0,
        ownerId = null,
        name = "",
        bio = null,
        username = null,
        groupType = GroupType.PRIVATE,
        removedUsers = 0,
        members = 0,
        isMember = false
    ),
    val originalChannelData: Group? = null,
    val hasChanges: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val pendingAvatarUri: Uri? = null,
    val permissions: ChatAdminPermissions = ChatAdminPermissions(),
    /** Показывается справа от пункта «Администраторы». */
    val adminsCount: Int = 0,
    /** Показывается справа от пункта «Заявки на вступление». */
    val joinRequestsCount: Int = 0
) {
    
    val isOwner: Boolean
        get() = permissions.isOwner
    
    val canEditProfile: Boolean
        get() = permissions.isOwner || permissions.canEditProfile
    
    val canManageInviteLinks: Boolean
        get() = permissions.isOwner || permissions.canManageInviteLinks
    
    /** Владелец управляет администраторами всегда, админ — только с отдельным разрешением. */
    val canManageAdmins: Boolean
        get() = permissions.isOwner || permissions.canManageAdmins
}
