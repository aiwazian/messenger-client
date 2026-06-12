/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.devices

import com.aiwazian.messenger.utils.UiText

sealed interface DevicesSideEffect {
    data class ShowSnackbar(val message: UiText) : DevicesSideEffect
}
