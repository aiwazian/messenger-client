/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class DnsProviderType(
    val primaryServer: String?,
    val secondaryServer: String?
) {
    SYSTEM(null, null),
    CLOUDFLARE("1.1.1.1", "1.0.0.1"),
    GOOGLE("8.8.8.8", "8.8.4.4"),
    ADGUARD("94.140.14.14", "94.140.15.15"),
    CUSTOM(null, null)
}
