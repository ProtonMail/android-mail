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

package ch.protonmail.android.mailcommon.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.DurationEpochTimeSample
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetCurrentEpochTimeDurationTest {

    private val getLocalisedCalendar: GetLocalisedCalendar = mockk()
    private val getCurrentEpochTimeDuration = GetCurrentEpochTimeDuration(
        getLocalisedCalendar = getLocalisedCalendar
    )

    @Test
    fun `when system time is Xms 2022 midnight, then correct epoch time is returned`() {
        // given
        every { getLocalisedCalendar().timeInMillis } returns 1_671_926_400_000L
        val expected = DurationEpochTimeSample.Y2022.Dec.D25.Midnight

        // when
        val actual = getCurrentEpochTimeDuration()

        // then
        assertEquals(expected, actual)
    }
}
