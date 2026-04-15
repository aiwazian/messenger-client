/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import com.aiwazian.messenger.domain.Group

data class GroupSettingsUiState(
    val isLoading: Boolean = false,
    val group: Group = Group(),
    val isDeleting: Boolean = false,
    val error: String? = null
)
