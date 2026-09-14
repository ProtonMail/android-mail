/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import ch.protonmail.android.mailcommon.domain.system.DeviceArchitectureProvider
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IsContentSearchFeatureEnabledTest {

    private val contentSearchFlag = mockk<FeatureFlag<Boolean>>()
    private val deviceArchitectureProvider = mockk<DeviceArchitectureProvider>()
    private val isContentSearchFeatureEnabled = IsContentSearchFeatureEnabled(
        contentSearchFlag,
        deviceArchitectureProvider
    )

    @Test
    fun `returns the flag value on a 64-bit process`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns true
        coEvery { contentSearchFlag.get() } returns true

        // When
        val result = isContentSearchFeatureEnabled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns false when the flag is off`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns true
        coEvery { contentSearchFlag.get() } returns false

        // When
        val result = isContentSearchFeatureEnabled()

        // Then
        assertFalse(result)
    }

    @Test
    fun `returns false without reading the flag on a 32-bit process`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns false

        // When
        val result = isContentSearchFeatureEnabled()

        // Then
        assertEquals(false, result)
        coVerify(exactly = 0) { contentSearchFlag.get() }
    }
}
