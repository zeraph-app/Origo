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

/** Forwards messages to the platform logger. */
object Log {
    @Volatile
    private var infoSink: (String) -> Unit = {}

    @Volatile
    private var warnSink: (String) -> Unit = {}

    fun install(info: (String) -> Unit, warn: (String) -> Unit) {
        infoSink = info
        warnSink = warn
    }

    fun info(message: String) = infoSink(message)

    fun warn(message: String) = warnSink(message)
}
