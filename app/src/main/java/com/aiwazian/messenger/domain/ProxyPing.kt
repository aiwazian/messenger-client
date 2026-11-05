/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

enum class ProxyPingStatus {
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

data class ProxyPing(
    val status: ProxyPingStatus,
    val pingMs: Long? = null
)
