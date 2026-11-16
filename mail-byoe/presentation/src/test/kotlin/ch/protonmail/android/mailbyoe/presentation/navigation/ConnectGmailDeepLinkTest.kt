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

package ch.protonmail.android.mailbyoe.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ConnectGmailDeepLinkTest {

    @Test
    fun `given the start link, when matching, then it matches`() {
        assertTrue(ConnectGmailDeepLink.matches("protonmail://connect-gmail/start"))
    }

    @Test
    fun `given another link, when matching, then it does not match`() {
        listOf(
            null,
            "",
            "ProtonMail://connect-gmail/start",
            "protonmail://connect-gmail/Start",
            "protonmail://connect-gmail/start/",
            "protonmail://connect-gmail/started",
            "protonmail://connect-gmail/start?source=test",
            "protonmail://connect-gmail/start#fragment",
            "protonmail://user@connect-gmail/start",
            "protonmail://connect-gmail:80/start",
            "protonmail://connect-gmail/complete?status=success",
            "https://connect-gmail/start"
        ).forEach { link ->
            assertFalse(ConnectGmailDeepLink.matches(link), "$link should not match")
        }
    }

    @Test
    fun `given a web page that lists the connected addresses, when matching, then it matches`() {
        listOf(
            "https://account.proton.me/mail/easy-switch",
            "https://account.proton.me/mail/easy-switch/",
            "https://account.proton.me/mail/easy-switch/gmail",
            "https://account.proton.me/mail/easy-switch?utm_source=newsletter&utm_campaign=byoe",
            "https://account.proton.me/mail/easy-switch#connect",
            "https://account.proton.me/mail/identity-addresses",
            "https://account.proton.me/mail/identity-addresses/add?source=email"
        ).forEach { link ->
            assertTrue(ConnectGmailDeepLink.matches(link), "$link should match")
        }
    }

    @Test
    fun `given another web page, when matching, then it does not match`() {
        listOf(
            "http://account.proton.me/mail/easy-switch",
            "https://proton.me/mail/easy-switch",
            "https://account.proton.me.example.com/mail/easy-switch",
            "https://user@account.proton.me/mail/easy-switch",
            "https://account.proton.me:8443/mail/easy-switch",
            "https://account.proton.me/mail/easy-switcher",
            "https://account.proton.me/mail/Easy-Switch",
            "https://account.proton.me/u/0/mail/easy-switch",
            "https://account.proton.me/mail",
            "https://account.proton.me/lite?action=byoe-mobile",
            "https://account.proton.me/mail/easy switch"
        ).forEach { link ->
            assertFalse(ConnectGmailDeepLink.matches(link), "$link should not match")
        }
    }
}
