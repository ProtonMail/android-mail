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

package ch.protonmail.android.maildetail.presentation

import java.time.Duration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import ch.protonmail.android.maildetail.presentation.util.toFormattedDurationParts
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

class DurationUtilsTest {

    val resourcesMock = mockk<Resources>()

    @Test
    fun `return empty list for non-positive Duration`() {
        // Given
        val duration = 0.minutes
        val expected = emptyList<String>()

        // When
        val actual = duration.toFormattedDurationParts(resourcesMock)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return empty list for Duration under 1 minute`() {
        // Given
        val duration = 59.seconds
        val expected = emptyList<String>()

        // When
        val actual = duration.toFormattedDurationParts(resourcesMock)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with only minutes`() {
        // Given
        val duration = 59.minutes
        val expected = listOf(
            expectPlurals(
                R.plurals.expiration_minutes_full_word,
                59,
                "59 minutes"
            )
        )

        // When
        val actual = duration.toFormattedDurationParts(resourcesMock)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with hours and minutes`() {
        // Given
        val duration = Duration.ofHours(1).plusMinutes(5).toKotlinDuration()
        val expected = listOf(
            expectPlurals(
                R.plurals.expiration_hours_full_word,
                1,
                "1 hour"
            ),
            expectPlurals(
                R.plurals.expiration_minutes_full_word,
                5,
                "5 minutes"
            )
        )

        // When
        val actual = duration.toFormattedDurationParts(resourcesMock)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with days, hours and minutes`() {
        // Given
        val duration = Duration.ofDays(2).plusHours(3).plusMinutes(4).toKotlinDuration()
        val expected = listOf(
            expectPlurals(
                R.plurals.expiration_days_full_word,
                2,
                "2 days"
            ),
            expectPlurals(
                R.plurals.expiration_hours_full_word,
                3,
                "3 hours"
            ),
            expectPlurals(
                R.plurals.expiration_minutes_full_word,
                4,
                "4 minutes"
            )
        )

        // When
        val actual = duration.toFormattedDurationParts(resourcesMock)

        // Then
        assertEquals(expected, actual)
    }

    private fun expectPlurals(
        @PluralsRes resourceId: Int,
        count: Int,
        expected: String
    ): String {
        every { resourcesMock.getQuantityString(resourceId, count, count) } returns expected

        return expected
    }
}
