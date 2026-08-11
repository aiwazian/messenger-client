/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.transfer

import com.aiwazian.messenger.utils.UiText

sealed interface GroupTransferOwnershipEffect {
    data object NavigateToMain : GroupTransferOwnershipEffect
    data class ShowSnackbar(val message: UiText) : GroupTransferOwnershipEffect
}
