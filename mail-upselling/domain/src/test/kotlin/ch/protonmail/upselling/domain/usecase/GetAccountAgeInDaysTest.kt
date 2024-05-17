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

package ch.protonmail.upselling.domain.usecase

import java.time.Instant
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.toUpsellingTelemetryDimensionValue
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import me.proton.core.user.domain.entity.User
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GetAccountAgeInDaysTest(private val testInput: TestInput) {

    private val getAccountAgeInDays = GetAccountAgeInDays()

    @BeforeTest
    fun setup() {
        mockkStatic(Instant::class)
    }

    @Test
    fun `should provide the correct value`() = with(testInput) {
        // Given
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns currentTime }

        // When
        val actual = getAccountAgeInDays(user).toUpsellingTelemetryDimensionValue()

        // Then
        assertEquals(expectedValue, actual)
    }

    companion object {

        private const val OneDayDuration = 86_400_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                currentTime = OneDayDuration * -1,
                expectedValue = "n/a"
            ),
            TestInput(
                currentTime = OneDayDuration * 0,
                expectedValue = "0"
            ),
            TestInput(
                currentTime = OneDayDuration * 1,
                expectedValue = "01-03"
            ),
            TestInput(
                currentTime = OneDayDuration * 2,
                expectedValue = "01-03"
            ),
            TestInput(
                currentTime = OneDayDuration * 3,
                expectedValue = "01-03"
            ),
            TestInput(
                currentTime = OneDayDuration * 4,
                expectedValue = "04-10"
            ),
            TestInput(
                currentTime = OneDayDuration * 7,
                expectedValue = "04-10"
            ),
            TestInput(
                currentTime = OneDayDuration * 10,
                expectedValue = "04-10"
            ),
            TestInput(
                currentTime = OneDayDuration * 11,
                expectedValue = "11-30"
            ),
            TestInput(
                currentTime = OneDayDuration * 23,
                expectedValue = "11-30"
            ),
            TestInput(
                currentTime = OneDayDuration * 30,
                expectedValue = "11-30"
            ),
            TestInput(
                currentTime = OneDayDuration * 31,
                expectedValue = "31-60"
            ),
            TestInput(
                currentTime = OneDayDuration * 45,
                expectedValue = "31-60"
            ),
            TestInput(
                currentTime = OneDayDuration * 60,
                expectedValue = "31-60"
            ),
            TestInput(
                currentTime = OneDayDuration * 61,
                expectedValue = ">60"
            )
        )
    }

    data class TestInput(
        val user: User = UserSample.Primary.copy(createdAtUtc = 0L),
        val currentTime: Long,
        val expectedValue: String
    )
}
