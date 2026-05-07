/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.utils.UiText

data class GroupTypeSettingsUiState(
    val groupId: Long = -1,
    val originalName: String = "",
    val username: String = "",
    val groupType: GroupType = GroupType.PRIVATE,
    val isError: Boolean = false,
    val canSave: Boolean = false,
    val statusText: UiText? = null
)
