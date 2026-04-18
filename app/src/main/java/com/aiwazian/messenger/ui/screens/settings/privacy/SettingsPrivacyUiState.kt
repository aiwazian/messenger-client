/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.enums.PrivacyLevel

data class SettingsPrivacyUiState(
    val privacy: PrivacySettings = PrivacySettings(
        bio = PrivacyLevel.EVERYBODY,
        dateOfBirth = PrivacyLevel.EVERYBODY,
        lastSeen = PrivacyLevel.EVERYBODY,
        messages = PrivacyLevel.EVERYBODY,
        invites = PrivacyLevel.EVERYBODY
    ),
    val showDeleteBottomSheet: Boolean = false,
    val showDeleteDialog: Boolean = false
)
