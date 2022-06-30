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

package ch.protonmail.android.mailsettings.presentation.settings.combinedcontacts

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCombinedContactsSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveCombinedContactsSetting
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CombinedContactsSettingViewModelTest {

    private val combinedContactsPreference = true
    private val combinedContactsPreferenceFlow = MutableSharedFlow<CombinedContactsPreference>()

    private val observeCombinedContactsSetting: ObserveCombinedContactsSetting = mockk {
        every { this@mockk() } returns combinedContactsPreferenceFlow
    }

    private val saveCombinedContactsSetting: SaveCombinedContactsSetting = mockk {
        justRun { this@mockk invoke "invoke" withArguments listOf(combinedContactsPreference) }
    }

    private val combinedContactsSettingViewModel by lazy {
        CombinedContactsSettingViewModel(
            observeCombinedContactsSetting,
            saveCombinedContactsSetting
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `should return loading state before the repository emits any value`() = runTest {
        combinedContactsSettingViewModel.state.test {
            val loadingState = awaitItem() as CombinedContactsSettingState.Loading
            assertEquals(CombinedContactsSettingState.Loading, loadingState)
        }
    }

    @Test
    fun `state should contain correct data when repository emits the preference`() = runTest {
        // Given
        val expectedResult = CombinedContactsSettingState.Data(isEnabled = true)
        val combinedContactsPreference = CombinedContactsPreference(isEnabled = true)

        combinedContactsSettingViewModel.state.test {
            awaitItem() as CombinedContactsSettingState.Loading

            // When
            combinedContactsPreferenceFlow.emit(combinedContactsPreference)

            // Then
            val dataState = awaitItem() as CombinedContactsSettingState.Data
            assertEquals(expectedResult, dataState)
        }
    }

    @Test
    fun `should call repository save method when saving combined contacts preference`() = runTest {
        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference)

        // Then
        coVerify { saveCombinedContactsSetting(combinedContactsPreference) }
    }
}
