/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import com.aiwazian.messenger.utils.UiText

sealed interface SelectChannelSideEffect {
    data object NavigateBack : SelectChannelSideEffect
    data class ShowSnackbar(val message: UiText) : SelectChannelSideEffect
}
