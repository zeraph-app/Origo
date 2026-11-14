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

import app.zeraph.origo.common.edge.ZeraphEdges
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import org.yaml.snakeyaml.Yaml

class Settings(
    val edgeOnly: Boolean,
    val edges: ZeraphEdges,
    private val legacyTrustedEdges: Boolean = false,
) {
    /** Hooks up logging and starts the edge list. */
    fun report(info: (String) -> Unit, warn: (String) -> Unit) {
        Log.install(info, warn)
        if (legacyTrustedEdges) {
            warn("trusted-edges in config.yml is no longer used: Origo now downloads Zeraph's edge list itself. You can delete it.")
        }
        edges.start()
        info("Recognising ${edges.size} Zeraph edge address range(s), refreshed from zeraph.app")
        info("Keep proxy protocol off for this backend in the Zeraph panel and Origo restores player IPs")
        if (edgeOnly) info("edge-only is on: connections that do not come through a Zeraph edge are refused")
    }

    companion object {
        fun load(directory: Path): Settings {
            val file = directory.resolve("config.yml")
            if (file.notExists()) {
                directory.createDirectories()
                Settings::class.java.getResourceAsStream("/config.yml")!!.use { Files.copy(it, file) }
            }

            val values = file.bufferedReader().use { Yaml().load<Map<String, Any?>>(it) }.orEmpty()
            return Settings(
                edgeOnly = values["edge-only"] as? Boolean ?: false,
                edges = ZeraphEdges(),
                legacyTrustedEdges = (values["trusted-edges"] as? List<*>).orEmpty().isNotEmpty(),
            )
        }
    }
}
