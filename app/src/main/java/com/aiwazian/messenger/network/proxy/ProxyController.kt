/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.proxy

import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import okhttp3.Credentials
import okhttp3.OkHttpClient

object ProxyController : ProxySelector() {
    @Volatile
    private var activeProxy: Proxy? = null

    @Volatile
    private var activeCredentials: ProxyServer? = null

    override fun select(uri: URI?): List<Proxy> {
        return listOfNotNull(activeProxy)
    }

    override fun connectFailed(uri: URI?, socketAddress: SocketAddress?, ioException: IOException?) {
    }

    fun activate(proxy: Proxy, credentials: ProxyServer?) {
        activeProxy = proxy
        activeCredentials = credentials
    }

    fun deactivate() {
        activeProxy = null
        activeCredentials = null
    }

    fun currentCredentials(): ProxyServer? = activeCredentials
}

fun OkHttpClient.Builder.applyAppProxy(): OkHttpClient.Builder {
    return proxySelector(ProxyController)
        .proxyAuthenticator { _, response ->
            val credentials = ProxyController.currentCredentials()
            if (response.code == 407 && credentials?.username != null) {
                response.request.newBuilder()
                    .header(
                        "Proxy-Authorization",
                        Credentials.basic(credentials.username, credentials.password.orEmpty())
                    )
                    .build()
            } else {
                null
            }
        }
}

fun ProxyServer.toJavaProxy(): Proxy {
    val type = when (this.type) {
        ProxyType.SOCKS5 -> Proxy.Type.SOCKS
        ProxyType.HTTPS -> Proxy.Type.HTTP
    }
    return Proxy(type, InetSocketAddress(host, port))
}
