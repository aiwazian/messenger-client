/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import com.aiwazian.messenger.domain.GroupAdmin

data class GroupAdminsUiState(
    val admins: List<GroupAdmin> = emptyList(),
    val isLoading: Boolean = false,
    val selectedUserId: Long? = null,
    val showDemoteDialog: Boolean = false
)
