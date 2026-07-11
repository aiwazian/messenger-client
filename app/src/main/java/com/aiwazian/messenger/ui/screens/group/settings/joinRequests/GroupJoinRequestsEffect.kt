/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.joinRequests

import com.aiwazian.messenger.utils.UiText

sealed interface GroupJoinRequestsEffect {
    data class ShowSnackbar(val message: UiText) : GroupJoinRequestsEffect
}
