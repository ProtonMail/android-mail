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

package ch.protonmail.android.mailevents.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AppInstallTimeRepositoryImplTest {

    private val packageInfo = PackageInfo().apply {
        firstInstallTime = 1_000_000_000_000L
    }

    private val packageManager = mockk<PackageManager> {
        every { getPackageInfo("test.package", 0) } returns packageInfo
    }

    private val context = mockk<Context> {
        every { packageManager } returns this@AppInstallTimeRepositoryImplTest.packageManager
        every { packageName } returns "test.package"
    }

    private val repository = AppInstallTimeRepositoryImpl(context)

    @Test
    fun `should return first install time from package manager`() {
        // When
        val result = repository.getFirstInstallTime()

        // Then
        assertEquals(1_000_000_000_000L, result)
    }
}
