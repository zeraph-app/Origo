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

package app.zeraph.origo.common.protocol

import io.netty.buffer.ByteBuf

/** Detects a PROXY protocol header (HAProxy protocol is on in Zeraph). */
object ProxyProtocol {
    private val V2_SIGNATURE = byteArrayOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A)
    private val V1_SIGNATURE = "PROXY ".toByteArray(Charsets.US_ASCII)

    enum class Detection { HEADER, NONE, INCOMPLETE }

    fun detect(buf: ByteBuf): Detection {
        for (signature in listOf(V2_SIGNATURE, V1_SIGNATURE)) {
            val length = minOf(buf.readableBytes(), signature.size)
            val matches = (0 until length).all { buf.getByte(buf.readerIndex() + it) == signature[it] }
            if (!matches) continue
            return if (length == signature.size) Detection.HEADER else Detection.INCOMPLETE
        }
        return Detection.NONE
    }
}
