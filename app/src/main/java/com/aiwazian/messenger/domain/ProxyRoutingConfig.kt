/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ProxyRoutingConfig(
    val use: Boolean = false,
    val activeProxyId: Long = -1
)
