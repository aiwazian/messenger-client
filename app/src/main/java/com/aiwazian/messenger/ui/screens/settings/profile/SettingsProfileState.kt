/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.compose.ui.graphics.Color
import com.aiwazian.messenger.domain.User

sealed interface SettingsProfileSideEffect {
    data object NavigateBack : SettingsProfileSideEffect
    data class ShowDatePicker(val initialDate: Long?) : SettingsProfileSideEffect
}

data class SettingsProfileUiState(
    val user: User = User(),
    val isSaving: Boolean = false,
    val showDatePicker: Boolean = false
)
