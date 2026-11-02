/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import com.aiwazian.messenger.enums.ProxyType

data class ProxyAddUiState(
    val type: ProxyType = ProxyType.SOCKS5,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val isEditing: Boolean = false
) {
    val canSave: Boolean
        get() {
            val portNumber = port.toIntOrNull()
            return host.isNotBlank() && portNumber != null && portNumber in 1..65535
        }
}
