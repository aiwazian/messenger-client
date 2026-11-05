/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.DnsProviderType

data class DnsConfig(
    val type: DnsProviderType = DnsProviderType.SYSTEM,
    val primaryDns: String = "",
    val secondaryDns: String = ""
)
