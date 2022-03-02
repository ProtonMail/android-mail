/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import ch.protonmail.android.mailsettings.domain.model.AppInformation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetAppInformationTest {

    private val packageInfo = PackageInfo()
    private val context = mockk<Context> {
        every { this@mockk.packageName } returns "ch.protonmail.android"
        every { this@mockk.packageManager } returns mockk packageManager@{
            every {
                this@packageManager.getPackageInfo(
                    "ch.protonmail.android",
                    PackageManager.GET_ACTIVITIES
                )
            } returns packageInfo
        }
    }

    private lateinit var getAppInformation: GetAppInformation

    @Before
    fun setUp() {
        getAppInformation = GetAppInformation(context)
    }

    @Test
    fun `returns app information with version from package manager`() {
        // Given
        packageInfo.versionName = "versionName-1.0.0"

        // When
        val actual = getAppInformation()

        // Then
        val expected = AppInformation("versionName-1.0.0")
        assertEquals(expected, actual)
    }
}
