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

package app.zeraph.origo.common.address

import java.net.InetAddress

class Cidr private constructor(private val network: ByteArray, private val prefix: Int) {

    operator fun contains(address: InetAddress): Boolean {
        val bytes = address.address
        if (bytes.size != network.size) return false

        val whole = prefix / 8
        for (index in 0 until whole) {
            if (bytes[index] != network[index]) return false
        }

        val remainder = prefix % 8
        if (remainder == 0) return true
        val mask = (0xFF shl (8 - remainder)) and 0xFF
        return (bytes[whole].toInt() and mask) == (network[whole].toInt() and mask)
    }

    override fun toString() = "${InetAddress.getByAddress(network).hostAddress}/$prefix"

    companion object {
        fun parse(value: String): Cidr {
            val host = value.substringBefore('/').trim()
            val address = requireNotNull(IpLiteral.parse(host)) { "Invalid IP address: $value" }
            val bits = address.address.size * 8
            val prefix = if ('/' in value) value.substringAfter('/').trim().toIntOrNull() else bits
            require(prefix != null && prefix in 0..bits) { "Invalid prefix length: $value" }
            return Cidr(address.address, prefix)
        }
    }
}
