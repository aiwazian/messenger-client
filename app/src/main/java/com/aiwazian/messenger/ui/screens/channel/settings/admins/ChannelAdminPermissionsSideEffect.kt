/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import com.aiwazian.messenger.utils.UiText

sealed interface ChannelAdminPermissionsSideEffect {
    data object NavigateBack : ChannelAdminPermissionsSideEffect
    data class ShowSnackbar(val message: UiText) : ChannelAdminPermissionsSideEffect
}
