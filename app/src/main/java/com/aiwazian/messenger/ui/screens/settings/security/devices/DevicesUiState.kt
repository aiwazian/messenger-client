/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.devices

import com.aiwazian.messenger.domain.Session

data class DevicesUiState(
    val sessions: List<Session> = emptyList(),
    val openedSession: Session? = null,
    val showTerminateSessionDialog: Boolean = false,
    val showTerminateAllOtherSessionsDialog: Boolean = false,
    val showSessionInfoBottomSheet: Boolean = false
)
