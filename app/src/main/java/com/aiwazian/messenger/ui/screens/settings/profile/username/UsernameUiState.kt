/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile.username

import com.aiwazian.messenger.utils.UiText

data class UsernameUiState(
    val originalName: String = "",
    val username: String = "",
    val isError: Boolean = false,
    val canSave: Boolean = false,
    val statusText: UiText? = null
)
