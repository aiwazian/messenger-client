/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.ProxyType

data class ProxyServer(
    val id: Long,
    val type: ProxyType,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?
)
