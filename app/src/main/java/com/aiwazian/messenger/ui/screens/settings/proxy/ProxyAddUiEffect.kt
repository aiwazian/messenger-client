/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import com.aiwazian.messenger.domain.ProxyServer

sealed interface ProxyAddUiEffect {
    data class Created(val proxy: ProxyServer) : ProxyAddUiEffect
}
