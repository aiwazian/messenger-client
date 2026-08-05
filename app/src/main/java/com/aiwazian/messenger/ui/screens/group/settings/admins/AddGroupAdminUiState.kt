/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import com.aiwazian.messenger.domain.User

data class AddGroupAdminUiState(
    val members: List<User> = emptyList()
)
