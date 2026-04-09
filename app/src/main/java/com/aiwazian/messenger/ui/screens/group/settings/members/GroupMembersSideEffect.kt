/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.members

sealed interface GroupMembersSideEffect {
    data class ShowSnackbar(val message: String) : GroupMembersSideEffect
    data object ShowKickConfirmation : GroupMembersSideEffect
    data object ShowBlockConfirmation : GroupMembersSideEffect
}
