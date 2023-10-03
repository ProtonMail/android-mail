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

package ch.protonmail.android.mailcommon.data.system

import android.webkit.WebView
import ch.protonmail.android.test.utils.mocks.WebViewProviderMocks.mockWebViewAvailabilityOnDevice
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DeviceCapabilitiesImplTest {

    @Before
    fun mockWebViewCheck() {
        mockkStatic(WebView::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Should return web view not available when provider is not present`() {
        // Given
        mockWebViewAvailabilityOnDevice(isPackagePresent = false)

        // When
        val capabilities = DeviceCapabilitiesImpl().getCapabilities()

        // Then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view not available when provider is present but not enabled`() {
        // Given
        mockWebViewAvailabilityOnDevice(isPackagePresent = true, isPackageEnabled = false)

        // When
        val capabilities = DeviceCapabilitiesImpl().getCapabilities()

        // Then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view available when provider is present and enabled`() {
        // Given
        mockWebViewAvailabilityOnDevice(isPackagePresent = true, isPackageEnabled = true)

        // When
        val capabilities = DeviceCapabilitiesImpl().getCapabilities()

        // Then
        assertEquals(true, capabilities.hasWebView)
    }
}
