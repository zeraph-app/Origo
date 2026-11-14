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

import app.zeraph.origo.common.Settings
import org.bukkit.plugin.java.JavaPlugin

class OrigoPaper : JavaPlugin() {

    override fun onEnable() {
        val settings = Settings.load(dataFolder.toPath())
        PaperInjector.register(settings)
        settings.report(logger::info, logger::warning)
    }

    override fun onDisable() {
        PaperInjector.unregister()
    }
}
