/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.networkmocks

import ch.protonmail.android.networkmocks.mockwebserver.requests.MimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequestLocalPath
import ch.protonmail.android.networkmocks.mockwebserver.requests.RemoteRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withMimeType
import ch.protonmail.android.networkmocks.mockwebserver.requests.withNetworkDelay
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("MaxLineLength")
internal class MockRequestTests {

    @Test
    fun `when mimeType is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest(path = "dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            mimeType = MimeType.OctetStream,
            ignoreQueryParams = true
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            mimeType = MimeType.Json,
            ignoreQueryParams = true
        )

        // When
        val actual = request withMimeType MimeType.OctetStream

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when ignoreQueryParams is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            ignoreQueryParams = true
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            ignoreQueryParams = false
        )

        // When
        val actual = request ignoreQueryParams true

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when matchWildcards is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            wildcardMatch = true
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            wildcardMatch = false
        )

        // When
        val actual = request matchWildcards true

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when networkDelay is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            networkDelay = 100L
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            networkDelay = 500L
        )

        // When
        val actual = request withNetworkDelay 100L

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when a priority is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            priority = MockPriority.Highest
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200
        )

        // When
        val actual = request withPriority MockPriority.Highest

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when serveOnce is set on a MockRequest, then the request is updated properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            serveOnce = true
        )

        val request = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 200,
            serveOnce = false
        )

        // When
        val actual = request serveOnce true

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when infix functions are used to generate a MockRequest, then its values are set properly`() {
        // Given
        val expected = MockRequest(
            remoteRequest = RemoteRequest("dummy-path"),
            localFilePath = MockRequestLocalPath("dummy-path-local"),
            statusCode = 201,
            ignoreQueryParams = true,
            mimeType = MimeType.Json,
            wildcardMatch = false,
            networkDelay = 1500L,
            serveOnce = true,
            simulateNoNetwork = false,
            priority = MockPriority.Highest
        )

        // When
        val actual =
            get("dummy-path") respondWith "dummy-path-local" withStatusCode 201 ignoreQueryParams true matchWildcards false serveOnce true withNetworkDelay 1500 withPriority MockPriority.Highest

        // Then
        assertEquals(expected, actual)
    }
}
