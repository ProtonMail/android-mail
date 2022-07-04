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

import java.io.IOException
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCombinedContactsSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveCombinedContactsSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombinedContactsSettingViewModelTest {

    private val combinedContactsPreference = CombinedContactsPreference(true)
    private val combinedContactsPreferenceFlow = MutableSharedFlow<CombinedContactsPreference>()

    private val observeCombinedContactsSetting: ObserveCombinedContactsSetting = mockk {
        every { this@mockk() } returns combinedContactsPreferenceFlow
    }

    private val saveCombinedContactsSetting: SaveCombinedContactsSetting = mockk()

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
            val loadingState = awaitItem()
            assertEquals(CombinedContactsSettingState.Loading, loadingState)
        }
    }

    @Test
    fun `state should contain correct data when repository emits the preference`() = runTest {
        // Given
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns Result.success(Unit)

        combinedContactsSettingViewModel.state.test {
            assertIs<CombinedContactsSettingState.Loading>(awaitItem())

            // When
            combinedContactsPreferenceFlow.emit(combinedContactsPreference)

            // Then
            val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertTrue(dataState.isEnabled)
            assertNull(dataState.combinedContactsSettingErrorEffect.consume())
        }
    }

    @Test
    fun `should call repository save method when saving combined contacts preference`() = runTest {
        // Given
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns Result.success(Unit)

        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

        // Then
        coVerify { saveCombinedContactsSetting(combinedContactsPreference.isEnabled) }
    }

    @Test
    fun `state should emit error data when an exception is thrown during saving combined contacts preference`() = runTest {
        // Given
        val ioException = IOException()
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns Result.failure(ioException)

        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

        // Then
        combinedContactsSettingViewModel.state.test {
            assertIs<CombinedContactsSettingState.Loading>(awaitItem())

            combinedContactsPreferenceFlow.emit(combinedContactsPreference)

            val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertEquals(ioException, dataState.combinedContactsSettingErrorEffect.consume())
        }
    }
}
