/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import com.aiwazian.messenger.utils.UiText

sealed interface GroupSettingsUiEffect {
    data object NavigateBack : GroupSettingsUiEffect
    data object NavigateToMain : GroupSettingsUiEffect
    data class ShowSnackbar(val message: UiText) : GroupSettingsUiEffect
}
