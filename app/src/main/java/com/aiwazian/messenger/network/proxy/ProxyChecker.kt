/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.proxy

import com.aiwazian.messenger.domain.ProxyPing
import com.aiwazian.messenger.domain.ProxyPingStatus
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProxyChecker @Inject constructor() {

    suspend fun check(proxy: ProxyServer): ProxyPing = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(proxy.host, proxy.port), CONNECT_TIMEOUT_MS)
                if (proxy.type == ProxyType.SOCKS5) {
                    performSocksHandshake(socket, proxy)
                }
            }
            ProxyPing(
                status = ProxyPingStatus.AVAILABLE,
                pingMs = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
            )
        } catch (_: Exception) {
            ProxyPing(status = ProxyPingStatus.UNAVAILABLE)
        }
    }

    private fun performSocksHandshake(socket: Socket, proxy: ProxyServer) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        output.write(
            byteArrayOf(SOCKS_VERSION, METHODS_COUNT, METHOD_NO_AUTH, METHOD_USERNAME_PASSWORD)
        )
        output.flush()

        if (input.read() != SOCKS_VERSION.toInt()) {
            throw IOException("Invalid socks greeting response")
        }
        when (input.read().toByte()) {
            METHOD_NO_AUTH -> return
            METHOD_USERNAME_PASSWORD -> performSocksAuth(input, output, proxy)
            else -> throw IOException("No acceptable socks auth method")
        }
    }

    private fun performSocksAuth(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        proxy: ProxyServer
    ) {
        val username = proxy.username.orEmpty().toByteArray()
        val password = proxy.password.orEmpty().toByteArray()
        if (username.isEmpty() || username.size > CREDENTIAL_MAX_LENGTH ||
            password.size > CREDENTIAL_MAX_LENGTH
        ) {
            throw IOException("Socks credentials are missing or too long")
        }

        output.write(
            byteArrayOf(
                AUTH_VERSION,
                username.size.toByte()
            ) + username + byteArrayOf(password.size.toByte()) + password
        )
        output.flush()

        if (input.read() != AUTH_VERSION.toInt() || input.read() != AUTH_SUCCESS) {
            throw IOException("Socks authentication failed")
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5000
        const val NANOS_PER_MILLI = 1_000_000L
        const val SOCKS_VERSION = 0x05.toByte()
        const val METHODS_COUNT = 0x02.toByte()
        const val METHOD_NO_AUTH = 0x00.toByte()
        const val METHOD_USERNAME_PASSWORD = 0x02.toByte()
        const val AUTH_VERSION = 0x01.toByte()
        const val AUTH_SUCCESS = 0x00
        const val CREDENTIAL_MAX_LENGTH = 255
    }
}
