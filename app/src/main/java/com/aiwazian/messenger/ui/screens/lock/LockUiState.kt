/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.lock

data class LockUiState(
    val passcode: String = "",
    val blockedUntil: Long = 0L,
    val remainingSeconds: Int = 0
)
