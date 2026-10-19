/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.deeplinks

import android.net.Uri
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkData
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkParser
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class NotificationsDeepLinkParserTest {

    private val validHost = NotificationsDeepLinkHelper.NotificationHost

    @Test
    fun `parseUri returns null when uri is null`() {
        assertNull(NotificationsDeepLinkParser.parseUri(null))
    }

    @Test
    fun `parseUri returns null when host does not match`() {
        val uri = deepLink("wrong-host", "mailbox/message/msg123/user456")
        assertNull(NotificationsDeepLinkParser.parseUri(uri))
    }

    @Test
    fun `parseUri returns Message for valid message deep link`() {
        val uri = deepLink(validHost, "mailbox/message/msg123/user456/notifId")

        val result = NotificationsDeepLinkParser.parseUri(uri) as NotificationsDeepLinkData.Message

        assertEquals("msg123", result.messageId)
        assertEquals("user456", result.userId)
    }

    @Test
    fun `parseUri returns Group for valid group deep link`() {
        val uri = deepLink(validHost, "mailbox/notif789/user456")

        val result = NotificationsDeepLinkParser.parseUri(uri) as NotificationsDeepLinkData.Group

        assertEquals("user456", result.userId)
    }

    @Test
    fun `parseUri returns null when 5 segments but not message type`() {
        val uri = deepLink(validHost, "mailbox/other/id123/user456/notifId")
        assertNull(NotificationsDeepLinkParser.parseUri(uri))
    }

    @Test
    fun `parseUri returns null when 3 segments but not mailbox prefix`() {
        val uri = deepLink(validHost, "inbox/notif789/user456")
        assertNull(NotificationsDeepLinkParser.parseUri(uri))
    }

    @Test
    fun `parseUri returns null for invalid path length`() {
        val uri = deepLink(validHost, "mailbox/message")
        assertNull(NotificationsDeepLinkParser.parseUri(uri))
    }

    // Uri.parse is a stub in the mockable android.jar; the parser only reads host and pathSegments.
    private fun deepLink(host: String, path: String): Uri = mockk {
        every { this@mockk.host } returns host
        every { pathSegments } returns path.split("/")
    }
}
