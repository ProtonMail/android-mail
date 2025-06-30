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

package ch.protonmail.android.mailupselling.domain.repository

import android.content.Intent
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GetInstalledProtonAppsTest {

    private val pm = mockk<PackageManager>()
    private val sut by lazy {
        GetInstalledProtonApps(
            mockk {
                every { this@mockk.packageManager } returns pm
            }
        )
    }

    @Test
    fun `returns empty set when no Proton apps are installed`() {
        // Given
        mockInstalled()

        // When
        val result = sut()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns only VPN when only VPN is installed`() {
        // Given
        mockInstalled(InstalledProtonApp.VPN)

        // When
        val result = sut()

        // Then
        assertEquals(setOf(InstalledProtonApp.VPN), result)
    }

    @Test
    fun `returns only Drive when only Drive is installed`() {
        // Given
        mockInstalled(InstalledProtonApp.Drive)

        // When
        val result = sut()

        // Then
        assertEquals(setOf(InstalledProtonApp.Drive), result)
    }

    @Test
    fun `returns only Calendar when only Calendar is installed`() {
        // Given
        mockInstalled(InstalledProtonApp.Calendar)

        // When
        val result = sut()

        // Then
        assertEquals(setOf(InstalledProtonApp.Calendar), result)
    }

    @Test
    fun `returns all Proton apps when all are installed`() {
        // Given
        mockInstalled(*InstalledProtonApp.entries.toTypedArray())

        // When
        val result = sut()

        // Then
        assertEquals(InstalledProtonApp.entries.toSet(), result)
    }

    private fun mockInstalled(vararg installedApps: InstalledProtonApp) {
        InstalledProtonApp.entries.forEach { app ->
            val pkg = when (app) {
                InstalledProtonApp.VPN -> "ch.protonvpn.android"
                InstalledProtonApp.Drive -> "me.proton.android.drive"
                InstalledProtonApp.Calendar -> "me.proton.android.calendar"
                InstalledProtonApp.Pass -> "proton.android.pass"
                InstalledProtonApp.Wallet -> "me.proton.wallet.android"
            }
            if (installedApps.contains(app)) {
                every { pm.getLaunchIntentForPackage(pkg) } returns mockk<Intent>()
            } else {
                every { pm.getLaunchIntentForPackage(pkg) } returns null
            }
        }
    }
}
