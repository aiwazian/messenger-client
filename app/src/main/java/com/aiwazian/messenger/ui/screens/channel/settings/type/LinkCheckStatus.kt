/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

sealed interface LinkCheckStatus {
    data object Idle : LinkCheckStatus
    data object Checking : LinkCheckStatus
    data object Available : LinkCheckStatus
    data object Busy : LinkCheckStatus
    data class Error(val message: String) : LinkCheckStatus
}
