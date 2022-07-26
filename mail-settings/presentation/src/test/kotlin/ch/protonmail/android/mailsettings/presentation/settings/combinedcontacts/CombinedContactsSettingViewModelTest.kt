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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCombinedContactsSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveCombinedContactsSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombinedContactsSettingViewModelTest {

    private val combinedContactsPreference = CombinedContactsPreference(true)

    private val observeCombinedContactsSetting: ObserveCombinedContactsSetting = mockk {
        every { this@mockk() } returns flowOf(combinedContactsPreference.right())
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
        // Given
        every { observeCombinedContactsSetting() } returns flow {
            delay(1) // simulate real delay
            emit(combinedContactsPreference.right())
        }

        // When
        combinedContactsSettingViewModel.state.test {
            // Then
            val loadingState = awaitItem()
            assertEquals(CombinedContactsSettingState.Loading, loadingState)
        }
    }

    @Test
    fun `state should contain correct data when repository emits the preference`() = runTest {
        // Given
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns Unit.right()

        // When
        combinedContactsSettingViewModel.state.test {
            // Then
            val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertTrue(dataState.isEnabled!!)
            assertNull(dataState.combinedContactsSettingErrorEffect.consume())
        }
    }

    @Test
    fun `state should contain correct data when an error occurs while observing the preference`() = runTest {
        // Given
        coEvery {
            observeCombinedContactsSetting()
        } returns flowOf(PreferencesError.left())

        // When
        combinedContactsSettingViewModel.state.test {
            // Then
            val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertNull(dataState.isEnabled)
            assertNotNull(dataState.combinedContactsSettingErrorEffect.consume())
        }
    }

    @Test
    fun `should call repository save method when saving combined contacts preference`() = runTest {
        // Given
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns Unit.right()

        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

        // Then
        coVerify { saveCombinedContactsSetting(combinedContactsPreference.isEnabled) }
    }

    @Test
    fun `state should emit error data when an exception is thrown during saving combined contacts preference`() =
        runTest {
            // Given
            coEvery {
                saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
            } returns PreferencesError.left()

            // When
            combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

            // Then
            combinedContactsSettingViewModel.state.test {
                val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
                assertNotNull(dataState.combinedContactsSettingErrorEffect.consume())
            }
        }

    @Test
    fun `can emit multiple errors`() = runTest {
        // Given
        coEvery {
            saveCombinedContactsSetting(combinedContactsPreference.isEnabled)
        } returns PreferencesError.left()

        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

        // Then
        combinedContactsSettingViewModel.state.test {
            val dataState = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertNotNull(dataState.combinedContactsSettingErrorEffect.consume())

            combinedContactsSettingViewModel.saveCombinedContactsPreference(combinedContactsPreference.isEnabled)

            val dataState2 = assertIs<CombinedContactsSettingState.Data>(awaitItem())
            assertNotNull(dataState2.combinedContactsSettingErrorEffect.consume())
        }
    }
}
