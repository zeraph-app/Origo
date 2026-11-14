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

import io.netty.buffer.Unpooled
import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyProtocolTest {
    private fun detect(bytes: ByteArray) = ProxyProtocol.detect(Unpooled.wrappedBuffer(bytes))

    @Test
    fun `recognises both header versions`() {
        assertEquals(ProxyProtocol.Detection.HEADER, detect("PROXY TCP4 1.2.3.4 5.6.7.8 1 2\r\n".toByteArray()))
        val v2 = byteArrayOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, 0x21)
        assertEquals(ProxyProtocol.Detection.HEADER, detect(v2))
    }

    @Test
    fun `waits on a partial signature and ignores handshakes`() {
        assertEquals(ProxyProtocol.Detection.INCOMPLETE, detect("PRO".toByteArray()))
        assertEquals(ProxyProtocol.Detection.NONE, detect(byteArrayOf(0x10, 0x00, 0x7F)))
        assertEquals(ProxyProtocol.Detection.NONE, detect(byteArrayOf(0x0D, 0x00)))
    }
}
