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

import app.zeraph.origo.common.address.IpLiteral
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.net.InetSocketAddress

/** The first packet of a Java edition connection. */
data class Handshake(
    val protocol: Int,
    val host: String,
    val port: Int,
    val nextState: Int,
    /** Packet size including its length prefix. */
    val frameLength: Int,
) {
    /** Splits an edge-appended player address off the hostname. */
    fun forwarded(): Forwarded? {
        val at = host.lastIndexOf(MARKER)
        if (at < 0) return null
        val payload = host.substring(at + MARKER.length)
        val address = payload.substringBeforeLast('/')
        val port = payload.substringAfterLast('/', "").toIntOrNull()?.takeIf { it in 0..65535 } ?: 0
        val ip = IpLiteral.parse(address)
        return Forwarded(host.substring(0, at), ip?.let { InetSocketAddress(it, port) })
    }

    /** Re-encodes the packet with another hostname. */
    fun withHost(newHost: String, allocator: ByteBufAllocator): ByteBuf {
        val body = allocator.buffer()
        try {
            body.writeVarInt(PACKET_ID)
            body.writeVarInt(protocol)
            body.writeString(newHost)
            body.writeShort(port)
            body.writeVarInt(nextState)
            val frame = allocator.buffer(body.readableBytes() + 3)
            frame.writeVarInt(body.readableBytes())
            frame.writeBytes(body)
            return frame
        } finally {
            body.release()
        }
    }

    data class Forwarded(val host: String, val address: InetSocketAddress?)

    sealed interface Read {
        data object Incomplete : Read
        data object NotHandshake : Read
        data class Parsed(val handshake: Handshake) : Read
    }

    companion object {
        /** Edges append `\0zeraph\0<ip>/<port>` to the hostname. */
        const val MARKER = "\u0000zeraph\u0000"
        private const val PACKET_ID = 0x00
        private const val LEGACY_PING = 0xFE
        /** Larger than any real handshake. */
        private const val MAX_FRAME = 2048
        private const val MAX_HOST_BYTES = 1024

        /** Reads the first packet without consuming it. */
        fun read(buf: ByteBuf): Read {
            if (!buf.isReadable) return Read.Incomplete
            if (buf.getUnsignedByte(buf.readerIndex()).toInt() == LEGACY_PING) return Read.NotHandshake

            val reader = buf.duplicate()
            val length = reader.readVarInt(3) ?: return if (reader.isReadable) Read.NotHandshake else Read.Incomplete
            if (length <= 0 || length > MAX_FRAME) return Read.NotHandshake
            val prefix = reader.readerIndex() - buf.readerIndex()
            if (reader.readableBytes() < length) return Read.Incomplete

            val packet = reader.readSlice(length)
            if (packet.readVarInt(5) != PACKET_ID) return Read.NotHandshake
            val protocol = packet.readVarInt(5) ?: return Read.NotHandshake
            val hostBytes = packet.readVarInt(3)?.takeIf { it in 0..MAX_HOST_BYTES } ?: return Read.NotHandshake
            if (packet.readableBytes() < hostBytes + 2) return Read.NotHandshake
            val host = packet.readCharSequence(hostBytes, Charsets.UTF_8).toString()
            val port = packet.readUnsignedShort()
            val nextState = packet.readVarInt(5) ?: return Read.NotHandshake
            if (packet.isReadable) return Read.NotHandshake

            return Read.Parsed(Handshake(protocol, host, port, nextState, prefix + length))
        }

        private fun ByteBuf.readVarInt(maxBytes: Int): Int? {
            var value = 0
            for (index in 0 until maxBytes) {
                if (!isReadable) return null
                val byte = readByte().toInt()
                value = value or ((byte and 0x7F) shl (7 * index))
                if (byte and 0x80 == 0) return value
            }
            return null
        }

        private fun ByteBuf.writeVarInt(value: Int) {
            var remaining = value
            while (true) {
                if (remaining and 0x7F.inv() == 0) {
                    writeByte(remaining)
                    return
                }
                writeByte((remaining and 0x7F) or 0x80)
                remaining = remaining ushr 7
            }
        }

        private fun ByteBuf.writeString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeVarInt(bytes.size)
            writeBytes(bytes)
        }
    }
}
