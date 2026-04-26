/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import com.aiwazian.messenger.utils.UiText

sealed interface GroupTypeSettingsEffect {
    object NavigateBack : GroupTypeSettingsEffect
    data class ShowSnackbar(val message: UiText) : GroupTypeSettingsEffect
}
