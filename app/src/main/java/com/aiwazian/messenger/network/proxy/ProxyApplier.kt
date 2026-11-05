/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.proxy

import com.aiwazian.messenger.di.ApplicationScope
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import com.aiwazian.messenger.repository.ProxyRepository
import com.aiwazian.messenger.utils.DataStoreManager
import java.net.Authenticator
import java.net.PasswordAuthentication
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Singleton
class ProxyApplier @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val proxyRepository: ProxyRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {
    fun start() {
        appScope.launch {
            combine(
                dataStoreManager.observeProxyRouting(),
                proxyRepository.observeProxies()
            ) { routing, proxies -> routing to proxies }
                .collect { (routing, proxies) ->
                    val active = proxies.firstOrNull { it.id == routing.activeProxyId }
                    if (routing.use && active != null) {
                        ProxyController.activate(active.toJavaProxy(), active)
                        installSocksAuthenticator(active)
                    } else {
                        ProxyController.deactivate()
                        clearSocksAuthenticator()
                    }
                }
        }
    }

    private fun installSocksAuthenticator(proxy: ProxyServer) {
        if (proxy.type != ProxyType.SOCKS5 || proxy.username.isNullOrBlank()) {
            clearSocksAuthenticator()
            return
        }

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                val matchesProxy = requestingProtocol == "SOCKS" &&
                        requestingHost == proxy.host &&
                        requestingPort == proxy.port
                return if (matchesProxy) {
                    PasswordAuthentication(proxy.username, proxy.password.orEmpty().toCharArray())
                } else {
                    null
                }
            }
        })
    }

    private fun clearSocksAuthenticator() {
        Authenticator.setDefault(null)
    }
}
