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

import app.zeraph.origo.common.Origo
import app.zeraph.origo.common.Settings
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer

class OrigoInitializer(
    private val settings: Settings,
    private val connectionHandler: String,
    private val delegate: ChannelInitializer<Channel>? = null,
) : ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        delegate?.let(channel.pipeline()::addLast)
        Origo.install(channel, settings, connectionHandler)
    }
}
