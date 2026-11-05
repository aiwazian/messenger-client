/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dns

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import okhttp3.Dns
import kotlin.random.Random

class UdpDnsResolver(
    private val primaryServer: InetAddress,
    private val secondaryServer: InetAddress?
) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        for (server in listOfNotNull(primaryServer, secondaryServer)) {
            try {
                return queryServer(hostname, server)
            } catch (_: Exception) {
            }
        }
        return Dns.SYSTEM.lookup(hostname)
    }

    private fun queryServer(hostname: String, server: InetAddress): List<InetAddress> {
        val addresses = queryType(hostname, server, TYPE_A) +
                queryType(hostname, server, TYPE_AAAA)
        if (addresses.isEmpty()) {
            throw UnknownHostException("No address records for $hostname from $server")
        }
        return addresses
    }

    private fun queryType(
        hostname: String,
        server: InetAddress,
        recordType: Int
    ): List<InetAddress> {
        val queryId = Random.nextInt(QUERY_ID_RANGE)
        val query = buildQuery(queryId, hostname, recordType)

        DatagramSocket().use { socket ->
            socket.soTimeout = QUERY_TIMEOUT_MS
            socket.send(DatagramPacket(query, query.size, server, DNS_PORT))

            val response = ByteArray(MAX_RESPONSE_SIZE)
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            return parseAnswers(packet.data, queryId, recordType)
        }
    }

    private fun buildQuery(queryId: Int, hostname: String, recordType: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val stream = DataOutputStream(output)

        stream.writeShort(queryId)
        stream.writeShort(RECURSION_DESIRED)
        stream.writeShort(QUESTION_COUNT)
        stream.writeShort(0)
        stream.writeShort(0)
        stream.writeShort(0)
        hostname.split('.').forEach { label ->
            if (label.isEmpty()) {
                return@forEach
            }
            stream.writeByte(label.length)
            stream.write(label.toByteArray(Charsets.US_ASCII))
        }
        stream.writeByte(0)
        stream.writeShort(recordType)
        stream.writeShort(INTERNET_CLASS)

        return output.toByteArray()
    }

    private fun parseAnswers(
        response: ByteArray,
        queryId: Int,
        recordType: Int
    ): List<InetAddress> {
        val buffer = ByteBuffer.wrap(response)
        if (buffer.getShort().toInt() != queryId) {
            throw UnknownHostException("Dns response id mismatch")
        }
        buffer.getShort()
        buffer.getShort()
        val answerCount = buffer.getShort().toInt() and 0xFFFF
        buffer.getShort()
        buffer.getShort()
        skipName(buffer)
        buffer.getShort()
        buffer.getShort()

        val addresses = mutableListOf<InetAddress>()
        repeat(answerCount) {
            skipName(buffer)
            val type = buffer.getShort().toInt() and 0xFFFF
            buffer.getShort()
            buffer.getInt()
            val dataLength = buffer.getShort().toInt() and 0xFFFF
            val data = ByteArray(dataLength)
            buffer.get(data)

            if (type == recordType && (dataLength == IPV4_LENGTH || dataLength == IPV6_LENGTH)) {
                runCatching { InetAddress.getByAddress(data) }
                    .getOrNull()
                    ?.let(addresses::add)
            }
        }
        return addresses
    }

    private fun skipName(buffer: ByteBuffer) {
        while (true) {
            val length = buffer.get().toInt() and 0xFF
            if (length == 0) {
                return
            }
            if (length and COMPRESSION_MASK == COMPRESSION_MASK) {
                buffer.get()
                return
            }
            buffer.position(buffer.position() + length)
        }
    }

    private companion object {
        const val DNS_PORT = 53
        const val QUERY_TIMEOUT_MS = 3000
        const val MAX_RESPONSE_SIZE = 4096
        const val QUERY_ID_RANGE = 65536
        const val RECURSION_DESIRED = 0x0100
        const val QUESTION_COUNT = 1
        const val INTERNET_CLASS = 1
        const val TYPE_A = 1
        const val TYPE_AAAA = 28
        const val IPV4_LENGTH = 4
        const val IPV6_LENGTH = 16
        const val COMPRESSION_MASK = 0xC0
    }
}
