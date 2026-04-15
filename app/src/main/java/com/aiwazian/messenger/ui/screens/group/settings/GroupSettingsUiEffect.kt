/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

sealed interface GroupSettingsUiEffect {
    data object NavigateBack : GroupSettingsUiEffect
    data object NavigateToMain : GroupSettingsUiEffect
    data class ShowError(val message: String) : GroupSettingsUiEffect
}
