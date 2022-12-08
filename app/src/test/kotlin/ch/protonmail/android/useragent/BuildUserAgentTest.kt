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

package ch.protonmail.android.useragent

import ch.protonmail.android.useragent.model.DeviceData
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BuildUserAgentTest {
    private val versionName = "6.0.0-alpha+fc6081a"
    private val androidVersion = "12"
    private val deviceModel = "model"
    private val deviceBrand = "brand"
    private val device = "device"

    private val getDeviceData = mockk<GetDeviceData> {
        every { this@mockk.invoke() } returns DeviceData(device, deviceBrand, deviceModel)
    }
    private val getAndroidVersion = mockk<GetAndroidVersion> {
        every { this@mockk.invoke() } returns androidVersion
    }
    private val getAppVersion = mockk<GetAppVersion> {
        every { this@mockk.invoke() } returns versionName
    }

    lateinit var buildUserAgent: BuildUserAgent

    @Before
    fun setUp() {
        buildUserAgent = BuildUserAgent(
            getAppVersion,
            getAndroidVersion,
            getDeviceData
        )
    }

    @Test
    fun `builds user agent correctly`() {
        val actual = buildUserAgent()

        val protonMail = "ProtonMail/$versionName"
        val android = "Android $androidVersion; $deviceBrand $deviceModel"
        val expected = "$protonMail ($android)"
        assertEquals(expected, actual)
    }
}
