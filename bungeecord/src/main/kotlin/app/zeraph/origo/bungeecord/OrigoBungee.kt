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

package app.zeraph.origo.bungeecord

import app.zeraph.origo.common.Origo
import app.zeraph.origo.common.Settings
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.protocol.channel.BungeeChannelInitializer

class OrigoBungee : Plugin() {

    override fun onEnable() {
        val settings = Settings.load(dataFolder.toPath())
        val unsafe = proxy.unsafe()
        val original = unsafe.frontendChannelInitializer

        unsafe.frontendChannelInitializer = BungeeChannelInitializer.create { channel ->
            Origo.install(channel, settings, connectionHandler = null, deferredInit = original.channelAcceptor::accept)
            true
        }
        settings.report(logger::info, logger::warning)
    }
}
