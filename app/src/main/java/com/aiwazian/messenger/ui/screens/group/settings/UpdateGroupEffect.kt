/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

sealed interface UpdateGroupEffect {
    data object NavigateBack : UpdateGroupEffect
    data object NavigateToMain : UpdateGroupEffect
}
