/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.management

import com.aiwazian.messenger.utils.UiText

sealed interface GroupManagementEffect {
    data object NavigateToMain : GroupManagementEffect
    data class ShowSnackbar(val message: UiText) : GroupManagementEffect
}
