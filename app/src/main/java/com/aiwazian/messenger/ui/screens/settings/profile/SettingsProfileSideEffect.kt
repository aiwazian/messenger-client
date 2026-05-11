/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import com.aiwazian.messenger.utils.UiText

sealed interface SettingsProfileSideEffect {
    data object NavigateBack : SettingsProfileSideEffect
    data class ShowSnackbar(val message: UiText) : SettingsProfileSideEffect
}
