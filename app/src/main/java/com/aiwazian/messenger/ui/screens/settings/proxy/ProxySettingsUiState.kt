/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import com.aiwazian.messenger.domain.ProxyPing
import com.aiwazian.messenger.domain.ProxyServer

data class ProxySettingsUiState(
    val proxies: List<ProxyServer> = emptyList(),
    val pings: Map<Long, ProxyPing> = emptyMap(),
    val useProxy: Boolean = false,
    val activeProxyId: Long? = null,
    val deleteTarget: ProxyServer? = null
)
