/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile.username

import com.aiwazian.messenger.utils.UiText

sealed interface UsernameUiEffect {
    data object NavigateBack : UsernameUiEffect
    data class ShowSnackbar(val message: UiText) : UsernameUiEffect
}
