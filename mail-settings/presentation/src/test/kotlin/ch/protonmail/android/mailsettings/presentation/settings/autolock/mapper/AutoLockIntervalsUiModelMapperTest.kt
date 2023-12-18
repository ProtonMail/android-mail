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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper

import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalListUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalSelectedUiModel
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class AutoLockIntervalsUiModelMapperTest {

    class BaseTest {

        private val autoLockIntervalsUiModelMapper = AutoLockIntervalsUiModelMapper()

        @Test
        fun `should return all the known intervals with the appropriate string res`() {
            // Given
            val expected = listOf(
                AutoLockIntervalListUiModel(AutoLockInterval.NotEnabled, R.string.mail_settings_auto_lock_not_enabled),
                AutoLockIntervalListUiModel(AutoLockInterval.Immediately, R.string.mail_settings_auto_lock_immediately),
                AutoLockIntervalListUiModel(
                    AutoLockInterval.FiveMinutes,
                    R.string.mail_settings_auto_lock_item_timer_description_five_minutes
                ),
                AutoLockIntervalListUiModel(
                    AutoLockInterval.FifteenMinutes,
                    R.string.mail_settings_auto_lock_item_timer_description_fifteen_minutes
                ),
                AutoLockIntervalListUiModel(
                    AutoLockInterval.OneHour,
                    R.string.mail_settings_auto_lock_item_timer_description_one_hour
                ),
                AutoLockIntervalListUiModel(
                    AutoLockInterval.OneDay,
                    R.string.mail_settings_auto_lock_item_timer_description_one_day
                ),
            )

            // When
            val actual = autoLockIntervalsUiModelMapper.toIntervalsListUiModel()

            // Then
            assertEquals(expected, actual)
        }
    }

    @RunWith(Parameterized::class)
    class ParameterizedMappingTest(private val testInput: TestInput) {

        private val autoLockIntervalsUiModelMapper = AutoLockIntervalsUiModelMapper()

        @Test
        fun `should map the selected interval to ui model interval`() = with(testInput) {
            // When
            val actual = autoLockIntervalsUiModelMapper.toSelectedIntervalUiModel(interval)

            // Then
            assertEquals(expectedValue, actual)
        }

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                TestInput(
                    interval = AutoLockInterval.NotEnabled,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.NotEnabled,
                        R.string.mail_settings_auto_lock_not_enabled
                    )
                ),
                TestInput(
                    interval = AutoLockInterval.Immediately,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.Immediately,
                        R.string.mail_settings_auto_lock_immediately
                    )
                ),
                TestInput(
                    interval = AutoLockInterval.FiveMinutes,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.FiveMinutes,
                        R.string.mail_settings_auto_lock_item_timer_description_five_minutes
                    )
                ),
                TestInput(
                    interval = AutoLockInterval.FifteenMinutes,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.FifteenMinutes,
                        R.string.mail_settings_auto_lock_item_timer_description_fifteen_minutes
                    )
                ),
                TestInput(
                    interval = AutoLockInterval.OneHour,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.OneHour,
                        R.string.mail_settings_auto_lock_item_timer_description_one_hour
                    )
                ),
                TestInput(
                    interval = AutoLockInterval.OneDay,
                    expectedValue = AutoLockIntervalSelectedUiModel(
                        AutoLockInterval.OneDay,
                        R.string.mail_settings_auto_lock_item_timer_description_one_day
                    )
                )
            )
        }

        data class TestInput(
            val interval: AutoLockInterval,
            val expectedValue: AutoLockIntervalSelectedUiModel
        )
    }
}
