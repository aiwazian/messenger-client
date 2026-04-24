/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import com.aiwazian.messenger.ui.components.topBar.TopBarAction

data class ProfileUiState(
    val profile: Profile? = null,
    val myId: Long = -1,
    val actions: List<TopBarAction> = emptyList(),
    val showLeaveDialog: Boolean = false
)
