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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PackageInfoFlags
import ch.protonmail.android.mailcommon.data.system.DeviceCapabilitiesImpl.Companion.WEB_VIEW_GOOGLE_PACKAGE
import ch.protonmail.android.mailcommon.data.system.DeviceCapabilitiesImpl.Companion.WEB_VIEW_PACKAGE
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DeviceCapabilitiesImplTest {

    private val applicationInfoMock = mockk<ApplicationInfo>(relaxed = true)
    private val packageInfoMock = mockk<PackageInfo>(relaxed = true)
    private val packageManagerMock = mockk<PackageManager>(relaxed = true) {
        every { this@mockk.getPackageInfo(any<String>(), any<Int>()) } returns packageInfoMock
        every { this@mockk.getPackageInfo(any<String>(), any<PackageInfoFlags>()) } returns packageInfoMock
    }
    private val context = mockk<Context>(relaxed = true) {
        every { this@mockk.packageManager } returns packageManagerMock
    }

    @Before
    fun setup() {
        packageInfoMock.applicationInfo = applicationInfoMock
    }

    @Test
    fun `Should return web view available when only the standard web view package is available and enabled`() {
        // given
        applicationInfoMock.enabled = true
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_PACKAGE, any<PackageInfoFlags>())
        } returns packageInfoMock
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_GOOGLE_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(true, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view not available when only the standard web view package is available but not enabled`() {
        // given
        applicationInfoMock.enabled = false
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_PACKAGE, any<PackageInfoFlags>())
        } returns packageInfoMock
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_GOOGLE_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view available when only the google web view package is available and enabled`() {
        // given
        applicationInfoMock.enabled = true
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_GOOGLE_PACKAGE, any<PackageInfoFlags>())
        } returns packageInfoMock

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(true, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view not available when only the google web view package is available but not enabled`() {
        // given
        applicationInfoMock.enabled = false
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_GOOGLE_PACKAGE, any<PackageInfoFlags>())
        } returns packageInfoMock

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view not available when none of the packages are available`() {
        // given
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()
        every {
            packageManagerMock.getPackageInfo(WEB_VIEW_GOOGLE_PACKAGE, any<PackageInfoFlags>())
        } throws NameNotFoundException()

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view not available when not enabled`() {
        // given
        applicationInfoMock.enabled = false

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(false, capabilities.hasWebView)
    }

    @Test
    fun `Should return web view available when enabled`() {
        // given
        applicationInfoMock.enabled = true

        // when
        val capabilities = DeviceCapabilitiesImpl(context).getCapabilities()

        // then
        assertEquals(true, capabilities.hasWebView)
    }
}
