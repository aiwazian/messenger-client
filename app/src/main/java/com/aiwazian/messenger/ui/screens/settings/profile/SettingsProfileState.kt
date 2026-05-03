/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import com.aiwazian.messenger.domain.User

sealed interface SettingsProfileSideEffect {
    data object NavigateBack : SettingsProfileSideEffect
}

data class SettingsProfileUiState(
    val user: User = User(
        id = 0,
        firstName = "",
        lastName = null,
        bio = null,
        username = null,
        dateOfBirth = null,
        lastSeen = null,
        avatars = emptyList()
    ),
    val showDatePicker: Boolean = false
)
