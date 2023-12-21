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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.AutoLockIntervalsUiModelMapper
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockEnabledUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockIntervalsUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class AutoLockSettingsReducerTest(
    private val testInput: TestInput
) {

    private val intervalsMapper = spyk<AutoLockIntervalsUiModelMapper>()
    private val reducer = AutoLockSettingsReducer(intervalsMapper)

    @Before
    fun setup() {
        every { intervalsMapper.toIntervalsListUiModel() } returns baseExpectedIntervals
        every { intervalsMapper.toSelectedIntervalUiModel(AutoLockInterval.Immediately) } returns immediatelyUiModel
        every { intervalsMapper.toSelectedIntervalUiModel(AutoLockInterval.OneDay) } returns oneDayUiModel
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should update the state according to the event`() = with(testInput) {
        // When
        val actual = reducer.newStateFrom(initialState, event)

        // Then
        assertEquals(expectedState, actual)
    }

    companion object {

        private val oneDayUiModel = AutoLockIntervalUiModel(AutoLockInterval.OneDay, description = 1)
        private val immediatelyUiModel = AutoLockIntervalUiModel(AutoLockInterval.Immediately, description = 0)

        private val baseExpectedIntervals = listOf(
            AutoLockIntervalUiModel(AutoLockInterval.Immediately, description = 0),
            AutoLockIntervalUiModel(AutoLockInterval.OneDay, description = 1)
        )

        private val baseAutoLockState = AutoLockSettingsState.DataLoaded.AutoLockEnabledState(
            AutoLockEnabledUiModel(true)
        )

        private val baseAutoLockIntervalState = AutoLockSettingsState.DataLoaded.AutoLockIntervalState(
            dropdownExpanded = false,
            autoLockIntervalsUiModel = AutoLockIntervalsUiModel(
                AutoLockIntervalUiModel(AutoLockInterval.Immediately, description = 0),
                baseExpectedIntervals
            )
        )

        private val baseDataLoaded = AutoLockSettingsState.DataLoaded(
            autoLockEnabledState = baseAutoLockState,
            autoLockIntervalsState = baseAutoLockIntervalState,
            Effect.empty(),
            Effect.empty(),
            Effect.empty()
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                AutoLockSettingsState.Loading,
                AutoLockSettingsEvent.Data.Loaded(AutoLockPreference(true), AutoLockInterval.Immediately),
                baseDataLoaded
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.Update.AutoLockPreferenceEnabled(newValue = false),
                baseDataLoaded.copy(autoLockEnabledState = baseAutoLockState.copy(AutoLockEnabledUiModel(false)))
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.Update.AutoLockIntervalSet(newValue = AutoLockInterval.OneDay),
                baseDataLoaded.copy(
                    autoLockIntervalsState = baseAutoLockIntervalState.copy(
                        AutoLockIntervalsUiModel(
                            oneDayUiModel,
                            baseExpectedIntervals
                        )
                    )
                )
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.UpdateError,
                baseDataLoaded.copy(updateError = Effect.of(Unit))
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.ForcePinCreation,
                baseDataLoaded.copy(forceOpenPinCreation = Effect.of(Unit))
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.ChangePinLockRequested,
                baseDataLoaded.copy(pinLockChangeRequested = Effect.of(Unit))
            ),
            TestInput(
                baseDataLoaded,
                AutoLockSettingsEvent.Update.AutoLockIntervalsDropDownToggled(true),
                baseDataLoaded.copy(autoLockIntervalsState = baseAutoLockIntervalState.copy(dropdownExpanded = true))
            ),
            TestInput(
                baseDataLoaded.copy(autoLockIntervalsState = baseAutoLockIntervalState.copy(dropdownExpanded = true)),
                AutoLockSettingsEvent.Update.AutoLockIntervalsDropDownToggled(false),
                baseDataLoaded.copy(autoLockIntervalsState = baseAutoLockIntervalState.copy(dropdownExpanded = false))
            )
        )
    }

    data class TestInput(
        val initialState: AutoLockSettingsState,
        val event: AutoLockSettingsEvent,
        val expectedState: AutoLockSettingsState
    )
}
