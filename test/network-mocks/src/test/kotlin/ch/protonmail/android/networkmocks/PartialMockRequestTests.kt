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

import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequestLocalPath
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockRequestRemotePath
import ch.protonmail.android.networkmocks.mockwebserver.requests.PartialMockRequest
import ch.protonmail.android.networkmocks.mockwebserver.requests.given
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PartialMockRequestTests {

    @Test
    fun `when infix function is applied on a String, then a PartialMockRequest is created`() {
        // Given
        val expected = PartialMockRequest(
            remotePath = MockRequestRemotePath("remotePath"),
            localFilePath = MockRequestLocalPath("localPath")
        )

        // When
        val actual = given("remotePath") respondWith "localPath"

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when a status code is added to a PartialMockRequest, then a MockRequest is created`() {
        // Given
        val expected = MockRequest(
            remotePath = MockRequestRemotePath("api/v1/remote-path"),
            localFilePath = MockRequestLocalPath("api/v1/local-path"),
            statusCode = 200
        )

        val partialMockRequest = PartialMockRequest(
            remotePath = MockRequestRemotePath("api/v1/remote-path"),
            localFilePath = MockRequestLocalPath("api/v1/local-path")
        )

        // When
        val actual = partialMockRequest withStatusCode 200

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when a status code is set via infix functions on a PartialMockRequest, then a MockRequest is created`() {
        // Given
        val expected = MockRequest(
            remotePath = MockRequestRemotePath("api/v1/remote-path"),
            localFilePath = MockRequestLocalPath("api/v1/local-path"),
            statusCode = 200
        )

        // When
        val actual = given("api/v1/remote-path") respondWith "api/v1/local-path" withStatusCode 200

        // Then
        assertEquals(expected, actual)
    }
}
