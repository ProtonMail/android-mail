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

package ch.protonmail.android.mailmessage.presentation.extension

import android.webkit.WebResourceRequest
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebResourceRequestExtensionTest {

    @Test
    fun `should return true when checking if http resource request is for remote content`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns "http"
        }

        // When
        val actual = webResourceRequest.isRemoteContent()

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when checking if https resource request is for remote content`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns "https"
        }

        // When
        val actual = webResourceRequest.isRemoteContent()

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when checking if resource request is for remote content and the url scheme is null`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns null
        }

        // When
        val actual = webResourceRequest.isRemoteContent()

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when checking if data resource request is for remote content`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns "data"
        }

        // When
        val actual = webResourceRequest.isRemoteContent()

        // Then
        assertFalse(actual)
    }

    @Test
    fun `isRemoteUnsecuredContent should return true for unsecured HTTP scheme`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns "http"
        }

        // When
        val actual = webResourceRequest.isRemoteUnsecuredContent()

        // Then
        assertTrue(actual)
    }

    @Test
    fun `isRemoteUnsecuredContent should return false for secured HTTPS scheme`() {
        // Given
        val webResourceRequest = mockk<WebResourceRequest> {
            every { url.scheme } returns "https"
        }

        // When
        val actual = webResourceRequest.isRemoteUnsecuredContent()

        // Then
        assertFalse(actual)
    }
}
