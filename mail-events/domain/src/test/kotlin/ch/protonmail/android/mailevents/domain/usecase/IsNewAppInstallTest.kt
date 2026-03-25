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

package ch.protonmail.android.mailevents.domain.usecase

import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IsNewAppInstallTest {

    private val appInstallRepository = mockk<AppInstallRepository>()

    private val isNewAppInstall = IsNewAppInstall(appInstallRepository)

    @Test
    fun `should return true when install time equals update time`() {
        // Given
        val installTime = 1000L
        every { appInstallRepository.getFirstInstallTime() } returns installTime
        every { appInstallRepository.getLastUpdateTime() } returns installTime

        // When
        val result = isNewAppInstall()

        // Then
        assertTrue(result)
    }

    @Test
    fun `should return false when install time differs from update time`() {
        // Given
        every { appInstallRepository.getFirstInstallTime() } returns 1000L
        every { appInstallRepository.getLastUpdateTime() } returns 2000L

        // When
        val result = isNewAppInstall()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return false when update time is newer than install time`() {
        // Given
        every { appInstallRepository.getFirstInstallTime() } returns 500L
        every { appInstallRepository.getLastUpdateTime() } returns 1500L

        // When
        val result = isNewAppInstall()

        // Then
        assertFalse(result)
    }
}
