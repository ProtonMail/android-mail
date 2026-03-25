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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.data.referrer.InstallReferrerDataSource
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AppInstallRepositoryImplTest {

    private val packageInfo = PackageInfo().apply {
        firstInstallTime = 1_000_000_000_000L
        lastUpdateTime = 2_000_000_000_000L
    }

    private val packageManager = mockk<PackageManager> {
        every { getPackageInfo("test.package", 0) } returns packageInfo
    }

    private val context = mockk<Context> {
        every { packageManager } returns this@AppInstallRepositoryImplTest.packageManager
        every { packageName } returns "test.package"
    }

    private val installReferrerDataSource = mockk<InstallReferrerDataSource>()

    private val repository = AppInstallRepositoryImpl(context, installReferrerDataSource)

    @Test
    fun `should return first install time from package manager`() {
        // When
        val result = repository.getFirstInstallTime()

        // Then
        assertEquals(1_000_000_000_000L, result)
    }

    @Test
    fun `should return last update time from package manager`() {
        // When
        val result = repository.getLastUpdateTime()

        // Then
        assertEquals(2_000_000_000_000L, result)
    }

    @Test
    fun `should return install referrer from data source`() = runTest {
        // Given
        val referrer = InstallReferrer(
            referrerUrl = "https://example.com",
            referrerClickTimestampMs = 1_000L,
            installBeginTimestampMs = 2_000L,
            isGooglePlayInstant = false
        )
        coEvery { installReferrerDataSource.getInstallReferrer() } returns referrer.right()

        // When
        val result = repository.getInstallReferrer()

        // Then
        assertEquals(referrer.right(), result)
    }

    @Test
    fun `should return error when install referrer fails`() = runTest {
        // Given
        val error = DataError.Remote.Unknown
        coEvery { installReferrerDataSource.getInstallReferrer() } returns error.left()

        // When
        val result = repository.getInstallReferrer()

        // Then
        assertEquals(error.left(), result)
    }
}
