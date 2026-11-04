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

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GmailConnectionCallbackTest {

    @Test
    fun `given a success callback, when parsing, then the account is connected`() {
        // When
        val result = parse(status = "success")

        // Then
        assertEquals(GmailConnectionResult.Connected(), result)
    }

    @Test
    fun `given a cancel callback, when parsing, then the connection is cancelled`() {
        // When
        val result = parse(status = "cancel")

        // Then
        assertEquals(GmailConnectionResult.Cancelled, result)
    }

    @Test
    fun `given an upsell callback, when parsing, then the upsell is requested`() {
        // When
        val result = parse(status = "upsell")

        // Then
        assertEquals(GmailConnectionResult.UpsellRequested, result)
    }

    @Test
    fun `given an error callback with a code, when parsing, then the code is kept`() {
        // When
        val result = parse(status = "error", code = "connection_limit")

        // Then
        assertEquals(GmailConnectionResult.Failed(code = "connection_limit"), result)
    }

    @Test
    fun `given an error callback with an unexpected code, when parsing, then the code is dropped`() {
        // When
        val result = parse(status = "error", code = "<script>alert(1)</script>")

        // Then
        assertEquals(GmailConnectionResult.Failed(code = null), result)
    }

    @Test
    fun `given an unknown status, when parsing, then the connection failed`() {
        // When
        val result = parse(status = "done")

        // Then
        assertEquals(GmailConnectionResult.Failed(code = null), result)
    }

    @Test
    fun `given a callback for another flow, when parsing, then the connection failed`() {
        // When
        val result = GmailConnectionCallback.parse(host = "login", path = "/complete", status = "success", code = null)

        // Then
        assertEquals(GmailConnectionResult.Failed(code = null), result)
    }

    @Test
    fun `given a success callback with the address, when parsing, then the address is kept`() {
        // When
        val result = parse(status = "success", email = "someone@gmail.com")

        // Then
        assertEquals(GmailConnectionResult.Connected(email = "someone@gmail.com"), result)
    }

    @Test
    fun `given a success callback with a malformed address, when parsing, then the address is dropped`() {
        // When
        val result = parse(status = "success", email = "not an <address>")

        // Then
        assertEquals(GmailConnectionResult.Connected(email = null), result)
    }

    private fun parse(
        status: String?,
        code: String? = null,
        email: String? = null
    ) = GmailConnectionCallback.parse(
        host = "connect-gmail",
        path = "/complete",
        status = status,
        code = code,
        email = email
    )
}
