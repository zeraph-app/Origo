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

package app.zeraph.origo.common

import app.zeraph.origo.common.pipeline.OrigoDecoder
import io.netty.channel.Channel

object Origo {
    const val MINECRAFT_CONNECTION = "packet_handler"

    fun install(
        channel: Channel,
        settings: Settings,
        connectionHandler: String?,
        deferredInit: ((Channel) -> Boolean)? = null,
    ) {
        channel.pipeline().addFirst(OrigoDecoder.NAME, OrigoDecoder(settings, connectionHandler, deferredInit))
    }
}
