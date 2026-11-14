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

package app.zeraph.origo.neoforge

import app.zeraph.origo.common.Origo
import app.zeraph.origo.common.Settings
import io.netty.channel.Channel
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths
import org.slf4j.LoggerFactory

@Mod(value = "origo", dist = [Dist.DEDICATED_SERVER])
class OrigoNeoForge {

    init {
        val logger = LoggerFactory.getLogger("Origo")
        settings = Settings.load(FMLPaths.CONFIGDIR.get().resolve("origo")).also { it.report(logger::info, logger::warn) }
    }

    companion object {
        @Volatile
        private var settings: Settings? = null

        @JvmStatic
        fun install(channel: Channel) {
            settings?.let { Origo.install(channel, it, Origo.MINECRAFT_CONNECTION) }
        }
    }
}
