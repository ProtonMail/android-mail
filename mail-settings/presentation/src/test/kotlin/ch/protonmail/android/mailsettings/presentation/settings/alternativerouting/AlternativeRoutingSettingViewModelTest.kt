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

package ch.protonmail.android.mailsettings.presentation.settings.alternativerouting

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAlternativeRoutingSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveAlternativeRoutingSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlternativeRoutingSettingViewModelTest {

    private val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true)

    private val observeAlternativeRoutingSetting: ObserveAlternativeRoutingSetting = mockk {
        every { this@mockk() } returns flowOf(alternativeRoutingPreference.right())
    }
    private val saveAlternativeRoutingSetting: SaveAlternativeRoutingSetting = mockk()

    private val alternativeRoutingSettingViewModel by lazy {
        AlternativeRoutingSettingViewModel(
            observeAlternativeRoutingSetting,
            saveAlternativeRoutingSetting
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `should return loading state before the repository emits any value`() = runTest {
        // Given
        every { observeAlternativeRoutingSetting() } returns flowOf()

        // When
        alternativeRoutingSettingViewModel.state.test {
            // Then
            val loadingState = awaitItem()
            assertEquals(AlternativeRoutingSettingState.Loading, loadingState)
        }
    }

    @Test
    fun `state should contain correct data when repository emits the preference`() = runTest {
        // When
        alternativeRoutingSettingViewModel.state.test {
            // Then
            val dataState = assertIs<AlternativeRoutingSettingState.Data>(awaitItem())
            assertTrue(dataState.isEnabled!!)
            assertNull(dataState.alternativeRoutingSettingErrorEffect.consume())
        }
    }

    @Test
    fun `state should contain correct data when an error occurs while observing the preference`() = runTest {
        // Given
        coEvery {
            observeAlternativeRoutingSetting()
        } returns flowOf(PreferencesError.left())

        // When
        alternativeRoutingSettingViewModel.state.test {
            // Then
            val dataState = assertIs<AlternativeRoutingSettingState.Data>(awaitItem())
            assertNull(dataState.isEnabled)
            assertNotNull(dataState.alternativeRoutingSettingErrorEffect.consume())
        }
    }

    @Test
    fun `should call repository save method when saving alternative routing preference`() = runTest {
        // Given
        coEvery {
            saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled)
        } returns Unit.right()

        // When
        alternativeRoutingSettingViewModel.saveAlternativeRoutingPreference(alternativeRoutingPreference.isEnabled)

        // Then
        coVerify { saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled) }
    }

    @Test
    fun `state should emit error data when an exception is thrown saving alternative routing preference`() = runTest {
        // Given
        coEvery {
            saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled)
        } returns PreferencesError.left()

        // When
        alternativeRoutingSettingViewModel.saveAlternativeRoutingPreference(alternativeRoutingPreference.isEnabled)

        // Then
        alternativeRoutingSettingViewModel.state.test {
            val dataState = assertIs<AlternativeRoutingSettingState.Data>(awaitItem())
            assertNotNull(dataState.alternativeRoutingSettingErrorEffect.consume())
        }
    }

    @Test
    fun `can emit multiple errors`() = runTest {
        // Given
        coEvery {
            saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled)
        } returns PreferencesError.left()

        // When
        alternativeRoutingSettingViewModel.saveAlternativeRoutingPreference(alternativeRoutingPreference.isEnabled)

        // Then
        alternativeRoutingSettingViewModel.state.test {
            val dataState = assertIs<AlternativeRoutingSettingState.Data>(awaitItem())
            assertNotNull(dataState.alternativeRoutingSettingErrorEffect.consume())

            alternativeRoutingSettingViewModel.saveAlternativeRoutingPreference(alternativeRoutingPreference.isEnabled)

            val dataState2 = assertIs<AlternativeRoutingSettingState.Data>(awaitItem())
            assertNotNull(dataState2.alternativeRoutingSettingErrorEffect.consume())
        }
    }
}
