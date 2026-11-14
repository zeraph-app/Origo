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

internal object IpLiteral {
    fun parse(value: String): InetAddress? = when {
        ':' in value -> parseV6(value)
        else -> parseV4(value)?.let(InetAddress::getByAddress)
    }

    private fun parseV4(value: String): ByteArray? {
        val parts = value.split('.')
        if (parts.size != 4) return null
        return ByteArray(4) { index ->
            val octet = parts[index]
            if (octet.isEmpty() || octet.length > 3 || !octet.all { it in '0'..'9' }) return null
            octet.toInt().takeIf { it <= 255 }?.toByte() ?: return null
        }
    }

    private fun parseV6(value: String): InetAddress? {
        val literal = value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' }
        if (!literal || value.first() == '.') return null
        return runCatching { InetAddress.getByName(value) }.getOrNull()
    }
}
