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

package app.zeraph.origo.common.edge

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZeraphEdgesTest {
    @Test
    fun `parses the published list and refuses anything else`() {
        assertEquals(2, ZeraphEdges.parse("153.56.229.9/32\n# comment\n\n2001:db8::/32\n")!!.size)
        assertNull(ZeraphEdges.parse("<!DOCTYPE html><html>Page not found</html>"))
        assertNull(ZeraphEdges.parse(""))
    }

    @Test
    fun `treats IPv4-mapped addresses as IPv4`() {
        assertEquals(InetAddress.getByName("1.2.3.4"), ZeraphEdges.unmapped(InetAddress.getByName("::ffff:1.2.3.4")))
        assertEquals(InetAddress.getByName("2001:db8::1"), ZeraphEdges.unmapped(InetAddress.getByName("2001:db8::1")))
    }
}
