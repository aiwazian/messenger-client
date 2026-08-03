/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import com.aiwazian.messenger.utils.UiText

sealed interface GroupAdminsSideEffect {
    data class ShowSnackbar(val message: UiText) : GroupAdminsSideEffect
}
