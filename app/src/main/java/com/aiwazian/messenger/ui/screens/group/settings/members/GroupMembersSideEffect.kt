/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

import com.aiwazian.messenger.utils.UiText

sealed interface GroupMembersSideEffect {
    data class ShowSnackbar(val message: UiText) : GroupMembersSideEffect
    data object ShowKickConfirmation : GroupMembersSideEffect
    data object ShowBlockConfirmation : GroupMembersSideEffect
}
