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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IsContentSearchFeatureEnabledTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRepository>()
    private val deviceArchitectureProvider = mockk<DeviceArchitectureProvider>()
    private val isContentSearchFeatureEnabled = IsContentSearchFeatureEnabled(
        repository,
        deviceArchitectureProvider
    )

    @Test
    fun `returns the availability reported for the account on a 64-bit process`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns true
        coEvery { repository.isFeatureEnabled(userId) } returns true

        // When
        val result = isContentSearchFeatureEnabled(userId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns false when the account reports the feature unavailable`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns true
        coEvery { repository.isFeatureEnabled(userId) } returns false

        // When
        val result = isContentSearchFeatureEnabled(userId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `returns false without asking the account on a 32-bit process`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns false

        // When
        val result = isContentSearchFeatureEnabled(userId)

        // Then
        assertEquals(false, result)
        coVerify(exactly = 0) { repository.isFeatureEnabled(any()) }
    }
}
