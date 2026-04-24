/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

sealed interface GroupTypeSettingsEffect {
    object NavigateBack : GroupTypeSettingsEffect
    data class ShowError(val message: String) : GroupTypeSettingsEffect
}
