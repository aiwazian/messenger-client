/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.lastSeen

sealed interface SettingsLastSeenEffect {
    data object Back : SettingsLastSeenEffect
}
