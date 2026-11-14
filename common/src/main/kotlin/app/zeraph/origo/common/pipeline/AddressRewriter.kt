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

import io.netty.channel.Channel
import java.lang.reflect.Modifier
import java.net.InetSocketAddress
import java.net.SocketAddress

internal object AddressRewriter {

    fun rewrite(channel: Channel, connectionHandler: String?, address: InetSocketAddress) {
        val original = channel.remoteAddress() ?: return
        listOfNotNull(connectionHandler?.let(channel.pipeline()::get), channel).forEach { replace(it, original, address) }
    }

    private fun replace(target: Any, original: SocketAddress, address: InetSocketAddress) {
        generateSequence<Class<*>>(target.javaClass) { it.superclass }
            .takeWhile { !it.module.isNamed }
            .flatMap { it.declaredFields.asSequence() }
            .filter { !Modifier.isStatic(it.modifiers) && it.type in ADDRESS_TYPES }
            .forEach { field ->
                field.isAccessible = true
                if (field.get(target) == original) field.set(target, address)
            }
    }

    private val ADDRESS_TYPES = setOf(SocketAddress::class.java, InetSocketAddress::class.java)
}
