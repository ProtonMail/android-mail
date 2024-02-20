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
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.util.toExpirationTextUiModels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

class DurationUtilsTest {

    @Test
    fun `return null for non-positive Duration`() {
        // Given
        val duration = 0.minutes
        val expected = null

        // When
        val actual = duration.toExpirationTextUiModels()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with only minutes`() {
        // Given
        val duration = 59.minutes
        val expected = listOf(
            TextUiModel(R.plurals.expiration_minutes_full_word, 59)
        )

        // When
        val actual = duration.toExpirationTextUiModels()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with hours and minutes`() {
        // Given
        val duration = Duration.ofHours(1).plusMinutes(5).toKotlinDuration()
        val expected = listOf(
            TextUiModel(R.plurals.expiration_hours_full_word, 1),
            TextUiModel(R.plurals.expiration_minutes_full_word, 5)
        )

        // When
        val actual = duration.toExpirationTextUiModels()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return expiration with days, hours and minutes`() {
        // Given
        val duration = Duration.ofDays(2).plusHours(3).plusMinutes(4).toKotlinDuration()
        val expected = listOf(
            TextUiModel(R.plurals.expiration_days_full_word, 2),
            TextUiModel(R.plurals.expiration_hours_full_word, 3),
            TextUiModel(R.plurals.expiration_minutes_full_word, 4)
        )

        // When
        val actual = duration.toExpirationTextUiModels()

        // Then
        assertEquals(expected, actual)
    }

}
