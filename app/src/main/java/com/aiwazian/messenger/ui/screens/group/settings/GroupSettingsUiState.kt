/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.enums.GroupType

data class GroupSettingsUiState(
    val group: Group = Group(
        id = 0,
        ownerId = null,
        name = "",
        bio = null,
        username = null,
        groupType = GroupType.PRIVATE,
        members = 0
    ),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)
