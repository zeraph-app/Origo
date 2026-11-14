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

package app.zeraph.origo.velocity

import app.zeraph.origo.common.Settings
import app.zeraph.origo.common.pipeline.OrigoInitializer
import com.velocitypowered.api.proxy.ProxyServer
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer

internal object VelocityInjector {
    private const val CONNECTION_HANDLER = "handler"

    fun inject(server: ProxyServer, settings: Settings) {
        val connectionManager = server.javaClass.getDeclaredField("cm").apply { isAccessible = true }.get(server)
        val holder = connectionManager.javaClass.getMethod("getServerChannelInitializer").invoke(connectionManager)

        @Suppress("UNCHECKED_CAST")
        val original = holder.javaClass.getMethod("get").invoke(holder) as ChannelInitializer<Channel>
        val wrapped = OrigoInitializer(settings, CONNECTION_HANDLER, original)
        holder.javaClass.getMethod("set", ChannelInitializer::class.java).invoke(holder, wrapped)
    }
}
