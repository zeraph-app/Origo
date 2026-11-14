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

package app.zeraph.origo.paper

import app.zeraph.origo.common.Origo
import app.zeraph.origo.common.Settings
import io.netty.channel.Channel
import net.kyori.adventure.key.Key
import java.lang.reflect.Proxy

internal object PaperInjector {
    private val KEY = Key.key("origo", "decoder")
    private val holder = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder")
    private val listenerType = Class.forName("io.papermc.paper.network.ChannelInitializeListener")

    fun register(settings: Settings) {
        val listener = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(listenerType)) { proxy, method, args ->
            when (method.name) {
                "afterInitChannel" -> Origo.install(args[0] as Channel, settings, Origo.MINECRAFT_CONNECTION)
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args[0]
                else -> "Origo"
            }
        }
        holder.getMethod("addListener", Key::class.java, listenerType).invoke(null, KEY, listener)
    }

    fun unregister() {
        holder.getMethod("removeListener", Key::class.java).invoke(null, KEY)
    }
}
