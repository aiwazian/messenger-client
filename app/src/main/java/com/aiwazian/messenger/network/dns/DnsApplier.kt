/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dns

import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.DnsConfig
import com.aiwazian.messenger.enums.DnsProviderType
import com.aiwazian.messenger.utils.DataStoreManager
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import okhttp3.Dns

@Singleton
class DnsApplier @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    @ApplicationScope private val appScope: CoroutineScope
) {
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            dataStoreManager.observeDnsConfig()
                .debounce(CONFIG_DEBOUNCE_MS)
                .collect { config ->
                    DnsController.activate(config.toDns())
                }
        }
    }

    private fun DnsConfig.toDns(): Dns {
        val serverAddresses = when (type) {
            DnsProviderType.SYSTEM -> return Dns.SYSTEM
            DnsProviderType.CLOUDFLARE,
            DnsProviderType.GOOGLE,
            DnsProviderType.ADGUARD -> listOfNotNull(type.primaryServer, type.secondaryServer)
            DnsProviderType.CUSTOM -> listOf(primaryDns, secondaryDns)
        }.mapNotNull { server -> server.toServerAddress() }

        val primary = serverAddresses.firstOrNull() ?: return Dns.SYSTEM
        return UdpDnsResolver(primary, serverAddresses.getOrNull(1))
    }

    private fun String.toServerAddress(): InetAddress? {
        if (!isValidIpv4(this)) {
            return null
        }
        return runCatching { InetAddress.getByName(this) }.getOrNull()
    }

    private fun isValidIpv4(value: String): Boolean {
        val octets = value.split('.')
        return octets.size == IPV4_OCTET_COUNT && octets.all { octet ->
            octet.isNotEmpty() && octet.length <= IPV4_OCTET_MAX_LENGTH &&
                    octet.all { it.isDigit() } &&
                    (octet == "0" || !octet.startsWith("0")) &&
                    octet.toInt() <= IPV4_OCTET_MAX_VALUE
        }
    }

    private companion object {
        const val CONFIG_DEBOUNCE_MS = 300L
        const val IPV4_OCTET_COUNT = 4
        const val IPV4_OCTET_MAX_LENGTH = 3
        const val IPV4_OCTET_MAX_VALUE = 255
    }
}
