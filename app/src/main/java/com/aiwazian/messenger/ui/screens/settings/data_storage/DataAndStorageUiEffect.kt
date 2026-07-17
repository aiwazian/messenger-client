/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.data_storage

import com.aiwazian.messenger.utils.UiText

sealed interface DataAndStorageUiEffect {
    data class ShowSnackbar(val message: UiText) : DataAndStorageUiEffect
}
