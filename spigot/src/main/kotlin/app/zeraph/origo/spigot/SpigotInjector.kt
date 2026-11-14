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

package app.zeraph.origo.spigot

import app.zeraph.origo.common.Origo
import app.zeraph.origo.common.Settings
import app.zeraph.origo.common.pipeline.OrigoInitializer
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.lang.reflect.Modifier
import org.bukkit.Bukkit

internal class SpigotInjector(settings: Settings) {
    private val acceptor = Acceptor(OrigoInitializer(settings, Origo.MINECRAFT_CONNECTION))
    private val channels = mutableListOf<Channel>()

    fun inject() {
        serverChannels().forEach { channel ->
            channel.pipeline().addFirst(ACCEPTOR, acceptor)
            channels += channel
        }
    }

    fun eject() {
        channels.forEach { channel -> runCatching { channel.pipeline().remove(ACCEPTOR) } }
        channels.clear()
    }

    private fun serverChannels(): List<Channel> {
        val server = Bukkit.getServer().let { it.javaClass.getMethod("getServer").invoke(it) }
        return generateSequence<Class<*>>(server.javaClass) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .filter { !Modifier.isStatic(it.modifiers) && it.type.name.startsWith("net.minecraft.") }
            .mapNotNull { it.apply { isAccessible = true }.get(server) }
            .firstNotNullOfOrNull(::channelFutures)
            ?.map(ChannelFuture::channel)
            ?: error("Could not locate the server network listener")
    }

    private fun channelFutures(listener: Any): List<ChannelFuture>? =
        listener.javaClass.declaredFields
            .filter { List::class.java.isAssignableFrom(it.type) }
            .map { it.apply { isAccessible = true }.get(listener) as? List<*> }
            .firstOrNull { list -> list.orEmpty().any { it is ChannelFuture } }
            ?.let { list -> synchronized(list) { list.filterIsInstance<ChannelFuture>() } }

    @Sharable
    private class Acceptor(private val initializer: OrigoInitializer) : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is Channel) msg.pipeline().addFirst(initializer)
            ctx.fireChannelRead(msg)
        }
    }

    private companion object {
        const val ACCEPTOR = "origo-acceptor"
    }
}
