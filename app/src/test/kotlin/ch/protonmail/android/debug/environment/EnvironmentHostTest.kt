/*
 * Copyright (c) 2026 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.debug.environment

import ch.protonmail.android.debug.environment.EnvironmentHost.Input
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EnvironmentHostTest {

    private companion object {

        const val DEV = "proton.black"
        const val ALPHA = "proton.me"
    }

    @Test
    fun `a proton black host is kept as it is`() {
        assertEquals(Input.Valid("maury.proton.black"), EnvironmentHost.parse("maury.proton.black", DEV))
    }

    @Test
    fun `a pasted link gives the environment host without the service label`() {
        assertEquals(
            Input.Valid("maury.proton.black"),
            EnvironmentHost.parse("https://account.maury.proton.black/login", DEV)
        )
    }

    @Test
    fun `links to other services of the environment give the same host`() {
        listOf(
            "https://mail.maury.proton.black/inbox",
            "https://calendar.maury.proton.black",
            "https://drive.maury.proton.black/",
            "https://pass.maury.proton.black/?x=1",
            "mail-api.maury.proton.black"
        ).forEach { input ->
            assertEquals(Input.Valid("maury.proton.black"), EnvironmentHost.parse(input, DEV), input)
        }
    }

    @Test
    fun `uppercase, whitespace and trailing slashes are ignored`() {
        listOf("  MAURY.PROTON.BLACK  ", "Maury.Proton.Black", "maury.proton.black/", "https://maury.proton.black///")
            .forEach { input ->
                assertEquals(Input.Valid("maury.proton.black"), EnvironmentHost.parse(input, DEV), input)
            }
    }

    @Test
    fun `the default environment and empty input mean the build default`() {
        listOf("", "   ", "proton.black", "https://account.proton.black/login", "account.proton.black")
            .forEach { input ->
                assertEquals(Input.Default, EnvironmentHost.parse(input, DEV), input)
            }
    }

    @Test
    fun `production can be typed or pasted`() {
        listOf("proton.me", "https://mail.proton.me/inbox", "account.proton.me", "PROTON.ME/").forEach { input ->
            assertEquals(Input.Valid("proton.me"), EnvironmentHost.parse(input, DEV), input)
        }
    }

    @Test
    fun `on a production build, its own host is the default and proton black is an override`() {
        assertEquals(Input.Default, EnvironmentHost.parse("proton.me", ALPHA))
        assertEquals(Input.Default, EnvironmentHost.parse("https://account.proton.me/login", ALPHA))
        assertEquals(Input.Valid("proton.black"), EnvironmentHost.parse("proton.black", ALPHA))
        assertEquals(Input.Valid("maury.proton.black"), EnvironmentHost.parse("maury.proton.black", ALPHA))
    }

    @Test
    fun `anything that is not a proton black host or production is invalid`() {
        listOf(
            "maury",
            "staging.protonmail.dev",
            "http://",
            "maury proton black",
            "maury.proton.me",
            "proton.ch",
            "a.b.proton.black",
            "maury_1",
            "https://evil.com/maury.proton.black"
        ).forEach { input ->
            assertEquals(Input.Invalid, EnvironmentHost.parse(input, DEV), input)
        }
    }

    @Test
    fun `the web app link opens the environment's sign-in page`() {
        assertEquals(
            "https://account.maury.proton.black/login",
            EnvironmentHost.accountLoginUrl("maury.proton.black")
        )
    }

    @Test
    fun `the default environment's web app link uses the build host`() {
        assertEquals("https://account.proton.black/login", EnvironmentHost.accountLoginUrl("proton.black"))
    }
}
