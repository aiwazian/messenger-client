/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dns

import java.net.InetAddress
import okhttp3.Dns

object DnsController : Dns {
    @Volatile
    private var delegate: Dns = Dns.SYSTEM

    override fun lookup(hostname: String): List<InetAddress> {
        return delegate.lookup(hostname)
    }

    fun activate(resolver: Dns) {
        delegate = resolver
    }
}
