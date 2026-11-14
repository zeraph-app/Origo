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
import io.netty.buffer.UnpooledByteBufAllocator
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HandshakeTest {
    @Test
    fun `round-trips a handshake`() {
        val built = Handshake(767, "play.example.com", 25565, 2, 0).withHost("play.example.com", UnpooledByteBufAllocator.DEFAULT)
        val length = built.readableBytes()
        val parsed = assertIs<Handshake.Read.Parsed>(Handshake.read(built)).handshake
        assertEquals(Handshake(767, "play.example.com", 25565, 2, length), parsed)
        assertEquals(0, built.readerIndex())
    }

    @Test
    fun `splits the forwarded address from the hostname`() {
        val forwarded = Handshake(767, "a.example.com${Handshake.MARKER}203.0.113.9/42", 25565, 2, 0).forwarded()!!
        assertEquals("a.example.com", forwarded.host)
        assertEquals(InetSocketAddress("203.0.113.9", 42), forwarded.address)
        assertNull(Handshake(767, "a.example.com", 25565, 2, 0).forwarded())
        assertNull(Handshake(767, "a.example.com${Handshake.MARKER}not-an-ip/1", 25565, 2, 0).forwarded()!!.address)
    }

    @Test
    fun `rejects what is not a handshake`() {
        assertIs<Handshake.Read.NotHandshake>(Handshake.read(Unpooled.wrappedBuffer(byteArrayOf(0xFE.toByte()))))
        assertIs<Handshake.Read.NotHandshake>(Handshake.read(Unpooled.wrappedBuffer(byteArrayOf(3, 0x05, 0, 0))))
        assertIs<Handshake.Read.NotHandshake>(Handshake.read(Unpooled.wrappedBuffer(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x7F))))
        assertIs<Handshake.Read.Incomplete>(Handshake.read(Unpooled.wrappedBuffer(byteArrayOf(20, 0))))
    }
}
