/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import com.aiwazian.messenger.ui.components.topBar.TopBarAction

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val myId: Long = 0,
    val actions: List<TopBarAction> = emptyList(),
    val error: String? = null,
    val showLeaveDialog: Boolean = false
)