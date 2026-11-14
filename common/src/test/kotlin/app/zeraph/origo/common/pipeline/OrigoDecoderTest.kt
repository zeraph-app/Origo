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

package app.zeraph.origo.common.pipeline

import app.zeraph.origo.common.Log
import app.zeraph.origo.common.Settings
import app.zeraph.origo.common.address.Cidr
import app.zeraph.origo.common.edge.ZeraphEdges
import app.zeraph.origo.common.protocol.Handshake
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.embedded.EmbeddedChannel
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrigoDecoderTest {

    private class Connection : ChannelInboundHandlerAdapter() {
        var address: SocketAddress? = null
        val received = ByteArrayOutputStream()

        override fun channelActive(ctx: ChannelHandlerContext) {
            address = ctx.channel().remoteAddress()
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            val buf = msg as ByteBuf
            val bytes = ByteArray(buf.readableBytes()).also(buf::readBytes)
            received.write(bytes)
            buf.release()
        }
    }

    private class RemoteChannel(private val remote: SocketAddress) : EmbeddedChannel() {
        override fun remoteAddress0(): SocketAddress = remote
    }

    private fun settings(edgeOnly: Boolean = false, edges: String = "198.51.100.0/24") =
        Settings(edgeOnly, ZeraphEdges().apply { replace(listOf(Cidr.parse(edges))) })

    private fun connect(remote: SocketAddress = EDGE, settings: Settings = settings()): Pair<RemoteChannel, Connection> {
        val connection = Connection()
        val channel = RemoteChannel(remote)
        channel.pipeline().addLast(OrigoDecoder.NAME, OrigoDecoder(settings, "packet_handler"))
        channel.pipeline().addLast("packet_handler", connection)
        channel.pipeline().fireChannelActive()
        return channel to connection
    }

    private fun handshake(host: String, nextState: Int = 2): ByteBuf =
        Handshake(767, host, 25565, nextState, 0).withHost(host, UnpooledByteBufAllocator.DEFAULT)

    private fun bytes(buf: ByteBuf): ByteArray = ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }

    /** The handshake as the server will read it. */
    private fun receivedHandshake(connection: Connection): Handshake =
        (Handshake.read(Unpooled.wrappedBuffer(connection.received.toByteArray())) as Handshake.Read.Parsed).handshake

    @Test
    fun `restores the player address forwarded by an edge`() {
        val (channel, connection) = connect()
        channel.writeInbound(handshake("play.example.com${Handshake.MARKER}203.0.113.9/51234"))

        val player = InetSocketAddress("203.0.113.9", 51234)
        assertEquals(player, connection.address)
        assertEquals(player, channel.remoteAddress())
        assertEquals("play.example.com", receivedHandshake(connection).host)
        assertNull(channel.pipeline().get(OrigoDecoder.NAME))
    }

    @Test
    fun `keeps a Forge suffix and handles IPv6 players`() {
        val (channel, connection) = connect()
        channel.writeInbound(handshake("play.example.com\u0000FML3\u0000${Handshake.MARKER}2001:db8::7/40000"))

        assertEquals(InetSocketAddress("2001:db8::7", 40000), connection.address)
        assertEquals("play.example.com\u0000FML3\u0000", receivedHandshake(connection).host)
    }

    @Test
    fun `strips a marker that does not come from an edge and keeps the real address`() {
        val stranger = InetSocketAddress("192.0.2.50", 40000)
        val (channel, connection) = connect(remote = stranger)
        channel.writeInbound(handshake("play.example.com${Handshake.MARKER}203.0.113.9/51234"))

        assertEquals(stranger, connection.address)
        assertEquals("play.example.com", receivedHandshake(connection).host)
        assertTrue(channel.isOpen)
    }

    @Test
    fun `ignores a marker on a server-list ping even from an edge`() {
        val (channel, connection) = connect()
        channel.writeInbound(handshake("play.example.com${Handshake.MARKER}6.6.6.6/1", nextState = 1))

        assertEquals(EDGE, connection.address)
        assertEquals("play.example.com", receivedHandshake(connection).host)
    }

    @Test
    fun `passes a handshake without a marker through byte for byte`() {
        val direct = InetSocketAddress("192.0.2.50", 1)
        val (channel, connection) = connect(remote = direct)
        val original = handshake("play.example.com")
        val expected = bytes(original)
        channel.writeInbound(original)

        assertEquals(direct, connection.address)
        assertContentEquals(expected, connection.received.toByteArray())
    }

    @Test
    fun `refuses an edge login that carries no player address`() {
        val (unmarked, _) = connect()
        unmarked.writeInbound(handshake("play.example.com"))
        assertFalse(unmarked.isOpen)

        val (garbled, _) = connect()
        garbled.writeInbound(handshake("play.example.com${Handshake.MARKER}not-an-ip/1"))
        assertFalse(garbled.isOpen)

        val (ping, connection) = connect()
        ping.writeInbound(handshake("play.example.com", nextState = 1))
        assertTrue(ping.isOpen)
        assertEquals(EDGE, connection.address)
    }

    @Test
    fun `keeps the packets that follow the handshake`() {
        val (channel, connection) = connect()
        val stream = Unpooled.buffer()
        stream.writeBytes(handshake("play.example.com${Handshake.MARKER}203.0.113.9/1"))
        stream.writeBytes(byteArrayOf(5, 0, 3, 'B'.code.toByte(), 'o'.code.toByte(), 'b'.code.toByte()))
        channel.writeInbound(stream)

        val received = Unpooled.wrappedBuffer(connection.received.toByteArray())
        val parsed = (Handshake.read(received) as Handshake.Read.Parsed).handshake
        received.skipBytes(parsed.frameLength)
        assertContentEquals(byteArrayOf(5, 0, 3, 'B'.code.toByte(), 'o'.code.toByte(), 'b'.code.toByte()), bytes(received))
    }

    @Test
    fun `waits for a handshake split across reads`() {
        val (channel, connection) = connect()
        val whole = bytes(handshake("play.example.com${Handshake.MARKER}203.0.113.9/7"))
        channel.writeInbound(Unpooled.wrappedBuffer(whole, 0, 5))
        assertEquals(0, connection.received.size())
        channel.writeInbound(Unpooled.wrappedBuffer(whole, 5, whole.size - 5))

        assertEquals(InetSocketAddress("203.0.113.9", 7), connection.address)
        assertEquals("play.example.com", receivedHandshake(connection).host)
    }

    @Test
    fun `recognises an edge reported as an IPv4-mapped IPv6 address`() {
        val mapped = InetSocketAddress(java.net.InetAddress.getByName("::ffff:198.51.100.20"), 40000)
        val (channel, connection) = connect(remote = mapped)
        channel.writeInbound(handshake("play.example.com${Handshake.MARKER}203.0.113.9/1"))
        assertEquals(InetSocketAddress("203.0.113.9", 1), connection.address)
    }

    @Test
    fun `edge-only refuses strangers and admits edges`() {
        val (stranger, _) = connect(remote = InetSocketAddress("192.0.2.50", 1), settings = settings(edgeOnly = true))
        stranger.writeInbound(handshake("play.example.com"))
        assertFalse(stranger.isOpen)

        val (edge, connection) = connect(settings = settings(edgeOnly = true))
        edge.writeInbound(handshake("play.example.com${Handshake.MARKER}203.0.113.9/1"))
        assertTrue(edge.isOpen)
        assertEquals("play.example.com", receivedHandshake(connection).host)
    }

    @Test
    fun `edge-only lets players in until the edge list has downloaded`() {
        val empty = Settings(true, ZeraphEdges())
        val (channel, connection) = connect(remote = InetSocketAddress("192.0.2.50", 1), settings = empty)
        channel.writeInbound(handshake("play.example.com"))

        assertTrue(channel.isOpen)
        assertEquals("play.example.com", receivedHandshake(connection).host)
    }

    @Test
    fun `steps aside for a PROXY protocol header and says so`() {
        val warnings = mutableListOf<String>()
        Log.install({}, { warnings += it })
        val (channel, connection) = connect()
        val header = "PROXY TCP4 203.0.113.9 198.51.100.20 5555 25565\r\n".toByteArray(Charsets.US_ASCII)
        channel.writeInbound(Unpooled.wrappedBuffer(header))

        assertContentEquals(header, connection.received.toByteArray())
        assertEquals(EDGE, connection.address)
        assertNull(channel.pipeline().get(OrigoDecoder.NAME))
        assertTrue(warnings.single().contains("HAProxy protocol turned on"))
    }

    @Test
    fun `edge-only also refuses a stranger who opens with a PROXY header`() {
        val (channel, connection) = connect(remote = InetSocketAddress("192.0.2.50", 1), settings = settings(edgeOnly = true))
        channel.writeInbound(Unpooled.wrappedBuffer("PROXY TCP4 6.6.6.6 1.1.1.1 1 25565\r\n".toByteArray(Charsets.US_ASCII)))

        assertFalse(channel.isOpen)
        assertEquals(0, connection.received.size())
    }

    @Test
    fun `closes an edge connection whose handshake cannot be parsed`() {
        val (edge, received) = connect()
        edge.writeInbound(handshake("a".repeat(1100) + "${Handshake.MARKER}203.0.113.9/1"))
        assertFalse(edge.isOpen)
        assertEquals(0, received.received.size())

        val (stranger, direct) = connect(remote = InetSocketAddress("192.0.2.50", 1))
        stranger.writeInbound(handshake("a".repeat(1100)))
        assertTrue(stranger.isOpen)
        assertTrue(direct.received.size() > 1100)
    }

    @Test
    fun `passes legacy server-list pings through`() {
        val (channel, connection) = connect()
        channel.writeInbound(Unpooled.wrappedBuffer(byteArrayOf(0xFE.toByte(), 0x01)))
        assertContentEquals(byteArrayOf(0xFE.toByte(), 0x01), connection.received.toByteArray())
    }

    @Test
    fun `defers platform setup until the real address is known`() {
        val connection = Connection()
        var seen: SocketAddress? = null
        val init = { channel: Channel ->
            seen = channel.remoteAddress()
            channel.pipeline().addLast("packet_handler", connection)
            true
        }
        val channel = RemoteChannel(EDGE)
        channel.pipeline().addLast(OrigoDecoder.NAME, OrigoDecoder(settings(), null, init))
        channel.pipeline().fireChannelActive()
        assertNull(seen)

        channel.writeInbound(handshake("play.example.com${Handshake.MARKER}203.0.113.9/5555"))

        val player = InetSocketAddress("203.0.113.9", 5555)
        assertEquals(player, seen)
        assertEquals(player, connection.address)
        assertEquals("play.example.com", receivedHandshake(connection).host)
    }

    private companion object {
        val EDGE = InetSocketAddress("198.51.100.20", 40000)
    }
}
