/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import com.aiwazian.messenger.utils.UiText

sealed interface ShareUiEffect {
    data class ShowToast(val message: UiText) : ShareUiEffect
    data object Close : ShareUiEffect
}
