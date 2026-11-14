/*
 * Copyright (C) 2026 Zeraph
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the MIT License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * MIT License for more details.
 *
 * You should have received a copy of the MIT License
 * along with this program.  If not, see <https://opensource.org/license/mit>.
 */

package app.zeraph.origo.common.edge

import app.zeraph.origo.common.Log
import app.zeraph.origo.common.address.Cidr
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Zeraph edge addresses, kept in memory and refreshed from zeraph.app. */
class ZeraphEdges {
    @Volatile
    private var ranges: List<Cidr> = emptyList()

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "origo-edge-list").apply { isDaemon = true }
    }

    val size get() = ranges.size

    operator fun contains(address: InetAddress): Boolean {
        val normalised = unmapped(address)
        return ranges.any { normalised in it }
    }

    /** Waits briefly for the first download. */
    fun start() {
        val first = scheduler.submit(::refresh)
        runCatching { first.get(STARTUP_WAIT_SECONDS, TimeUnit.SECONDS) }
        scheduler.schedule(::keepFresh, 0, TimeUnit.SECONDS)
    }

    fun stop() {
        scheduler.shutdownNow()
    }

    internal fun replace(next: List<Cidr>) {
        ranges = next
    }

    private fun keepFresh() {
        val next = if (ranges.isEmpty() && !refresh()) RETRY_SECONDS else REFRESH_SECONDS
        if (!scheduler.isShutdown) scheduler.schedule(::keepFresh, next, TimeUnit.SECONDS)
    }

    private fun refresh(): Boolean {
        val fetched = runCatching { download() }
            .onFailure { Log.warn("Could not download the Zeraph edge list: ${it.message}") }
            .getOrNull()
        if (fetched.isNullOrEmpty()) return false

        if (fetched.map(Cidr::toString).toSet() != ranges.map(Cidr::toString).toSet()) {
            Log.info("Zeraph edge list updated: ${fetched.size} address range(s)")
        }
        ranges = fetched
        return true
    }

    private fun download(): List<Cidr> {
        val request = HttpRequest.newBuilder(URI.create(SOURCE))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "Origo")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val bytes = response.body().use { it.readNBytes(MAX_BYTES + 1) }
        check(response.statusCode() == 200) { "$SOURCE answered ${response.statusCode()}" }
        check(bytes.size <= MAX_BYTES) { "$SOURCE is larger than $MAX_BYTES bytes" }
        return parse(bytes.decodeToString()) ?: error("$SOURCE is not an address list")
    }

    companion object {
        private const val SOURCE = "https://zeraph.app/edges-v4"
        private const val MAX_BYTES = 64 * 1024
        private const val STARTUP_WAIT_SECONDS = 5L
        private const val RETRY_SECONDS = 30L
        private const val REFRESH_SECONDS = 15 * 60L

        /** One range per line; null if any line isn't. */
        fun parse(text: String): List<Cidr>? {
            val lines = text.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            if (lines.isEmpty()) return null
            return lines.map { line -> runCatching { Cidr.parse(line) }.getOrNull() ?: return null }
        }

        /** Maps ::ffff:a.b.c.d back to IPv4. */
        fun unmapped(address: InetAddress): InetAddress {
            if (address !is Inet6Address) return address
            val bytes = address.address
            val mapped = (0 until 10).all { bytes[it] == 0.toByte() } && bytes[10] == 0xFF.toByte() && bytes[11] == 0xFF.toByte()
            return if (mapped) Inet4Address.getByAddress(bytes.copyOfRange(12, 16)) else address
        }
    }
}
