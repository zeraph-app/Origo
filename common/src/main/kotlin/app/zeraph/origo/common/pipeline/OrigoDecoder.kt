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
import app.zeraph.origo.common.protocol.Handshake
import app.zeraph.origo.common.protocol.ProxyProtocol
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Restores the player address an edge appended to the handshake; only trusted from Zeraph edges. */
class OrigoDecoder(
    private val settings: Settings,
    private val connectionHandler: String?,
    private val deferredInit: ((Channel) -> Boolean)? = null,
) : ByteToMessageDecoder() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (deferredInit == null) {
            super.channelActive(ctx)
        } else {
            ctx.executor().schedule({ if (!ctx.isRemoved) ctx.close() }, HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        when (ProxyProtocol.detect(input)) {
            ProxyProtocol.Detection.INCOMPLETE -> return
            ProxyProtocol.Detection.HEADER -> {
                if (refuses(ctx.fromEdge())) return reject(ctx, input)
                warnProxyProtocol()
                return complete(ctx, input)
            }
            ProxyProtocol.Detection.NONE -> Unit
        }

        when (val read = Handshake.read(input)) {
            Handshake.Read.Incomplete -> if (input.readableBytes() > MAX_BUFFERED) unparsed(ctx, input)
            Handshake.Read.NotHandshake -> unparsed(ctx, input)
            is Handshake.Read.Parsed -> handle(ctx, input, out, read.handshake)
        }
    }

    private fun handle(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>, handshake: Handshake) {
        val fromEdge = ctx.fromEdge()
        if (refuses(fromEdge)) return reject(ctx, input)

        val forwarded = handshake.forwarded()
        val edgeLogin = fromEdge && handshake.nextState in LOGIN_STATES

        // An edge login without a player address would show up as the edge's IP, so it is closed.
        if (edgeLogin && forwarded?.address == null) {
            warnOnce(lastUnmarkedWarning, "Refused a login from a Zeraph edge that carried no player address")
            return reject(ctx, input)
        }
        if (forwarded == null) return complete(ctx, input)

        // Always strip the marker; trust it only on edge logins, the only handshakes edges mark.
        if (edgeLogin) {
            forwarded.address?.let { AddressRewriter.rewrite(ctx.channel(), connectionHandler, it) }
        }
        input.skipBytes(handshake.frameLength)
        out.add(handshake.withHost(forwarded.host, ctx.alloc()))
        complete(ctx, input)
    }

    /** An edge peer we can't parse would show up as the edge's IP, so it is closed; others pass. */
    private fun unparsed(ctx: ChannelHandlerContext, input: ByteBuf) {
        val fromEdge = ctx.fromEdge()
        val legacyPing = input.getUnsignedByte(input.readerIndex()).toInt() == LEGACY_PING
        if (refuses(fromEdge) || (fromEdge && !legacyPing)) return reject(ctx, input)
        complete(ctx, input)
    }

    /** edge-only lets everyone in until the edge list has downloaded, rather than locking players out. */
    private fun refuses(fromEdge: Boolean): Boolean {
        if (!settings.edgeOnly || fromEdge) return false
        if (settings.edges.size > 0) return true
        warnOnce(lastEmptyListWarning, "edge-only is on but the Zeraph edge list has not downloaded yet, so connections are let in")
        return false
    }

    private fun complete(ctx: ChannelHandlerContext, input: ByteBuf) {
        deferredInit?.let { init ->
            if (!init(ctx.channel())) return reject(ctx, input)
            ctx.fireChannelActive()
        }
        ctx.pipeline().remove(this)
    }

    private fun reject(ctx: ChannelHandlerContext, input: ByteBuf) {
        input.skipBytes(input.readableBytes())
        ctx.close()
    }

    private fun ChannelHandlerContext.fromEdge(): Boolean {
        val remote = channel().remoteAddress() as? InetSocketAddress ?: return false
        return remote.address in settings.edges
    }

    companion object {
        const val NAME = "origo-decoder"
        private const val HANDSHAKE_TIMEOUT_SECONDS = 30L
        private const val MAX_BUFFERED = 4096
        private const val LEGACY_PING = 0xFE
        private val LOGIN_STATES = setOf(2, 3)
        private val WARN_EVERY_MS = TimeUnit.HOURS.toMillis(1)
        private val lastProxyWarning = AtomicLong(0)
        private val lastEmptyListWarning = AtomicLong(0)
        private val lastUnmarkedWarning = AtomicLong(0)

        /** Logs at most once an hour per kind of warning. */
        private fun warnOnce(last: AtomicLong, message: String, now: Long = System.currentTimeMillis()) {
            val previous = last.get()
            if (previous != 0L && now - previous < WARN_EVERY_MS) return
            if (last.compareAndSet(previous, now)) Log.warn(message)
        }

        internal fun warnProxyProtocol() {
            warnOnce(
                lastProxyWarning,
                "This server's backend has HAProxy protocol turned on in Zeraph, so Origo is no longer needed. " +
                    "Either turn on your server's own PROXY protocol setting (Paper proxies.proxy-protocol, " +
                    "Velocity haproxy-protocol, BungeeCord proxy_protocol) and remove Origo, or turn HAProxy " +
                    "protocol off for this backend in the Zeraph panel and keep Origo.",
            )
        }
    }
}
