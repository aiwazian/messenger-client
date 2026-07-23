/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

import com.aiwazian.messenger.ui.components.CodeInputStatus

data class LockUiState(
    val passcode: String = "",
    val blockedUntil: Long = 0L,
    val remainingSeconds: Int = 0,
    val status: CodeInputStatus = CodeInputStatus.Default,
    val fingerprintEnabled: Boolean = false
)
