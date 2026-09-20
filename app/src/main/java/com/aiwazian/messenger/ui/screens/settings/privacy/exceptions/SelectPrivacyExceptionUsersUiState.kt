/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.exceptions

import com.aiwazian.messenger.domain.Chat

data class SelectPrivacyExceptionUsersUiState(
    val query: String = "",
    val users: List<Chat> = emptyList(),
    val selectedUserIds: Set<Long> = emptySet()
) {

    val hasSelection: Boolean
        get() = selectedUserIds.isNotEmpty()
}
