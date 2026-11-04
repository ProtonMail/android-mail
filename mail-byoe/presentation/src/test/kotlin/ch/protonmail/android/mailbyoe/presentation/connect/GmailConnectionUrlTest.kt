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

package ch.protonmail.android.mailbyoe.presentation.connect

import ch.protonmail.android.mailsession.domain.model.CookieSessionId
import ch.protonmail.android.mailsession.domain.model.ForkWithKeyPayload
import ch.protonmail.android.mailsession.domain.model.Selector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GmailConnectionUrlTest {

    @Test
    fun `given a fork, when building the url, then the fork data is only in the fragment`() {
        // Given
        val fork = ForkWithKeyPayload(
            selector = Selector("selector"),
            sessionId = CookieSessionId("session"),
            payloadKey = "key",
            payloadVersion = 3,
            payloadType = "default"
        )

        // When
        val url = gmailConnectionUrl(
            apiHost = "proton.me",
            appVersion = "android-mail@7.12.0",
            fork = fork,
            importEmails = true
        )

        // Then
        assertEquals(
            "https://account.proton.me/lite?action=byoe-mobile&app-version=android-mail@7.12.0" +
                "&autoconnect=1&importEmails=1" +
                "&redirect=protonmail%3A%2F%2Fconnect-gmail%2Fcomplete" +
                "#selector=selector&sk=key&pv=3&pt=default",
            url
        )
    }

    @Test
    fun `given the user turned off the import, when building the url, then the page connects without importing`() {
        // Given
        val fork = ForkWithKeyPayload(
            selector = Selector("selector"),
            sessionId = CookieSessionId("session"),
            payloadKey = "key",
            payloadVersion = 3,
            payloadType = "default"
        )

        // When
        val url = gmailConnectionUrl(
            apiHost = "proton.me",
            appVersion = "android-mail@7.12.0",
            fork = fork,
            importEmails = false
        )

        // Then
        assertTrue(url.contains("&autoconnect=1&importEmails=0&"))
    }
}
