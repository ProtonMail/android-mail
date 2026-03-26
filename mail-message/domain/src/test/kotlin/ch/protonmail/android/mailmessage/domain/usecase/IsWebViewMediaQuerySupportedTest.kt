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
package ch.protonmail.android.mailmessage.domain.usecase

import android.content.Context
import android.content.pm.PackageInfo
import androidx.webkit.WebViewCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class IsWebViewMediaQuerySupportedTest {

    private lateinit var context: Context
    private lateinit var useCase: IsWebViewMediaQuerySupported

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        useCase = IsWebViewMediaQuerySupported(context)
        mockkStatic(WebViewCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(WebViewCompat::class)
    }

    @Test
    fun `returns true when webview package is null`() {
        // Given
        every { WebViewCompat.getCurrentWebViewPackage(context) } returns null

        // When
        val result = useCase()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns true when version name cannot be parsed`() {
        // Given
        val packageInfo = PackageInfo().apply {
            versionName = "invalid.version"
        }
        every { WebViewCompat.getCurrentWebViewPackage(context) } returns packageInfo

        // When
        val result = useCase()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns true when major version is greater than minimum supported version`() {
        // Given
        val packageInfo = PackageInfo().apply {
            versionName = "120.0.6099.144"
        }
        every { WebViewCompat.getCurrentWebViewPackage(context) } returns packageInfo

        // When
        val result = useCase()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns false when major version is lower than minimum supported version`() {
        // Given
        val packageInfo = PackageInfo().apply {
            versionName = "99.0.4844.84"
        }
        every { WebViewCompat.getCurrentWebViewPackage(context) } returns packageInfo

        // When
        val result = useCase()

        // Then
        assertFalse(result)
    }

    @Test
    fun `returns true when getting current webview package throws exception`() {
        // Given
        every { WebViewCompat.getCurrentWebViewPackage(context) } throws RuntimeException("exception")

        // When
        val result = useCase()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns true when version name is null`() {
        // Given
        val packageInfo = PackageInfo().apply {
            versionName = null
        }
        every { WebViewCompat.getCurrentWebViewPackage(context) } returns packageInfo

        // When
        val result = useCase()

        // Then
        assertTrue(result)
    }
}
