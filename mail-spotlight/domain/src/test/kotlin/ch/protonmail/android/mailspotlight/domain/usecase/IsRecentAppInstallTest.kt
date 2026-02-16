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

package ch.protonmail.android.mailspotlight.domain.usecase

import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal class IsRecentAppInstallTest {

    private val appInstallRepository = mockk<AppInstallRepository>()
    private val clock = mockk<Clock>()

    private val isRecentAppInstall = IsRecentAppInstall(
        appInstallRepository = appInstallRepository,
        clock = clock
    )

    @Test
    fun `should return true when app was installed less than 1 day ago`() {
        // Given
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000L)
        val installTime = now.minus(12.hours).toEpochMilliseconds()

        every { clock.now() } returns now
        every { appInstallRepository.getFirstInstallTime() } returns installTime

        // When
        val result = isRecentAppInstall()

        // Then
        assertTrue(result)
    }

    @Test
    fun `should return false when app was installed more than 1 day ago`() {
        // Given
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000L)
        val installTime = now.minus(2.days).toEpochMilliseconds()

        every { clock.now() } returns now
        every { appInstallRepository.getFirstInstallTime() } returns installTime

        // When
        val result = isRecentAppInstall()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return false when app was installed exactly 1 day ago`() {
        // Given
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000L)
        val installTime = now.minus(1.days).toEpochMilliseconds()

        every { clock.now() } returns now
        every { appInstallRepository.getFirstInstallTime() } returns installTime

        // When
        val result = isRecentAppInstall()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should use custom threshold when provided`() {
        // Given
        val now = Instant.fromEpochMilliseconds(1_000_000_000_000L)
        val installTime = now.minus(6.hours).toEpochMilliseconds()

        every { clock.now() } returns now
        every { appInstallRepository.getFirstInstallTime() } returns installTime

        // When
        val result = isRecentAppInstall(threshold = 12.hours)

        // Then
        assertTrue(result)
    }
}
