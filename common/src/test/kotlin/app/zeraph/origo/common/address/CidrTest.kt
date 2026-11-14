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

package app.zeraph.origo.common.address

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CidrTest {

    @Test
    fun `matches ranges`() {
        val range = Cidr.parse("203.0.113.0/27")
        assertTrue(InetAddress.getByName("203.0.113.31") in range)
        assertFalse(InetAddress.getByName("203.0.113.32") in range)
        assertFalse(InetAddress.getByName("2001:db8::1") in range)
    }

    @Test
    fun `treats a bare address as a single host`() {
        val host = Cidr.parse("2001:db8::1")
        assertTrue(InetAddress.getByName("2001:db8::1") in host)
        assertFalse(InetAddress.getByName("2001:db8::2") in host)
    }

    @Test
    fun `rejects invalid input`() {
        assertFailsWith<IllegalArgumentException> { Cidr.parse("10.0.0.0/33") }
        assertFailsWith<IllegalArgumentException> { Cidr.parse("edge.example.com") }
    }
}
