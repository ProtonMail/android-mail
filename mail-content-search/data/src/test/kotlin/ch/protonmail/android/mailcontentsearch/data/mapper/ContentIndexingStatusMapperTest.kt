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

package ch.protonmail.android.mailcontentsearch.data.mapper

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import io.mockk.every
import io.mockk.mockk
import uniffi.mail_uniffi.ContentSearchIndexingProgress
import uniffi.mail_uniffi.ContentSearchIndexingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ContentIndexingStatusMapperTest {

    private fun progress(status: ContentSearchIndexingStatus, fraction: Double?) =
        mockk<ContentSearchIndexingProgress> {
            every { this@mockk.status } returns status
            every { estimatedFraction } returns fraction
        }

    @Test
    fun `maps NONE status to Idle`() {
        assertEquals(ContentIndexingState.Idle, toIndexingState(ContentSearchIndexingStatus.NONE, null))
    }

    @Test
    fun `maps COMPLETED status to Completed`() {
        assertEquals(ContentIndexingState.Completed, toIndexingState(ContentSearchIndexingStatus.COMPLETED, null))
    }

    @Test
    fun `maps INTERRUPTED status to Cancelled`() {
        assertEquals(ContentIndexingState.Cancelled, toIndexingState(ContentSearchIndexingStatus.INTERRUPTED, null))
    }

    @Test
    fun `maps ONGOING status to Running with the progress percentage`() {
        // Given
        val ongoing = progress(ContentSearchIndexingStatus.ONGOING, fraction = 0.42)

        // When
        val result = toIndexingState(ContentSearchIndexingStatus.ONGOING, ongoing)

        // Then
        assertEquals(ContentIndexingState.Running(42.0), result)
    }

    @Test
    fun `maps ONGOING status with no fraction to Running at zero percent`() {
        // When
        val result = toIndexingState(ContentSearchIndexingStatus.ONGOING, null)

        // Then
        assertEquals(ContentIndexingState.Running(0.0), result)
    }

    @Test
    fun `progress extension derives the state from its own status`() {
        // Given
        val ongoing = progress(ContentSearchIndexingStatus.ONGOING, fraction = 1.0)

        // When + Then
        assertEquals(ContentIndexingState.Running(100.0), ongoing.toIndexingState())
    }

    @Test
    fun `toPercentage converts the estimated fraction to a percentage`() {
        assertEquals(75.0, progress(ContentSearchIndexingStatus.ONGOING, fraction = 0.75).toPercentage())
    }

    @Test
    fun `toPercentage defaults to zero when the fraction is missing`() {
        assertEquals(0.0, progress(ContentSearchIndexingStatus.ONGOING, fraction = null).toPercentage())
    }

    @Test
    fun `terminal statuses are reported as terminal`() {
        assertTrue(ContentSearchIndexingStatus.NONE.isTerminal())
        assertTrue(ContentSearchIndexingStatus.COMPLETED.isTerminal())
        assertTrue(ContentSearchIndexingStatus.INTERRUPTED.isTerminal())
    }

    @Test
    fun `ongoing status is not terminal`() {
        assertFalse(ContentSearchIndexingStatus.ONGOING.isTerminal())
    }
}
