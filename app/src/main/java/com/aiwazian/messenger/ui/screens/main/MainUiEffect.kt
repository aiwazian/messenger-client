/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

sealed class MainUiEffect {
    data object ShowPermissionRationale : MainUiEffect()
    data object HidePermissionRationale : MainUiEffect()
    data object OpenNotificationSettings : MainUiEffect()
}