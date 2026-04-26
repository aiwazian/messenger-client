/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.screens.channel.settings.type.LinkCheckStatus

data class GroupTypeSettingsUiState(
    val groupId: Long = -1,
    val username: String = "",
    val groupType: GroupType = GroupType.PRIVATE,
    val linkCheckStatus: LinkCheckStatus = LinkCheckStatus.Idle,
    val canSave: Boolean = false,
)
