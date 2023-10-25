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

package ch.protonmail.android.mailcommon.presentation.mapper

import ch.protonmail.android.mailcommon.domain.sample.DurationEpochTimeSample
import ch.protonmail.android.mailcommon.domain.usecase.GetCurrentEpochTimeDuration
import ch.protonmail.android.mailcommon.presentation.R.string
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class ExpirationTimeMapperTest {

    private val now = DurationEpochTimeSample.Y2022.Dec.D25.Midnight
    private val getCurrentEpochTimeDuration: GetCurrentEpochTimeDuration = mockk {
        every { this@mockk() } returns now
    }
    private val mapper = ExpirationTimeMapper(getCurrentEpochTimeDuration)

    @Test
    fun `when expiration is in the past, then expired is returned`() {
        // given
        val expiration = now - 10.minutes
        val expected = TextUiModel(string.expiration_expired)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `when expiration in 10 minutes, then 10 minutes is returned`() {
        // given
        val expiration = now + 10.minutes
        val expected = TextUiModel(value = string.expiration_minutes_arg, 10)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `when expiration in 1 hour, then 1 hour is returned`() {
        // given
        val expiration = now + 1.hours
        val expected = TextUiModel(value = string.expiration_hours_arg, 1)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `when expiration in 1 hour 10 minutes, then 1 hour is returned`() {
        // given
        val expiration = now + 1.hours + 10.minutes
        val expected = TextUiModel(value = string.expiration_hours_arg, 1)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `when expiration is 1 day, then 1 day is returned`() {
        // given
        val expiration = now + 1.days
        val expected = TextUiModel(value = string.expiration_days_arg, 1)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun `when expiration is 1 year, then 365 days is returned`() {
        // given
        val expiration = now + 365.days
        val expected = TextUiModel(value = string.expiration_days_arg, 365)

        // when
        val actual = mapper.toUiModel(expiration)

        // then
        assertEquals(expected, actual)
    }
}
