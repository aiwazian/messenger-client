/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.dns

import com.aiwazian.messenger.enums.DnsProviderType

data class DnsSettingsUiState(
    val type: DnsProviderType = DnsProviderType.SYSTEM,
    val primaryDns: String = "",
    val secondaryDns: String = ""
)
