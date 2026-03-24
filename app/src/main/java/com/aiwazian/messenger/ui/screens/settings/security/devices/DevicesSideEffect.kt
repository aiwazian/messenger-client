/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.devices

sealed interface DevicesSideEffect {
    data class ShowSnackbar(val message: String) : DevicesSideEffect
    object VibrateError : DevicesSideEffect
}
