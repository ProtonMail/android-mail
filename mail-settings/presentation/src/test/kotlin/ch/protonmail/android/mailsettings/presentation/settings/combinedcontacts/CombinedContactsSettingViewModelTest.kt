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
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CombinedContactsSettingViewModelTest {

    private val combinedContactsPreferenceFlow = MutableSharedFlow<CombinedContactsPreference>()

    private val combinedContactsRepository: CombinedContactsRepository = mockk {
        coEvery { observe() } returns combinedContactsPreferenceFlow
    }

    private val combinedContactsSettingViewModel by lazy {
        CombinedContactsSettingViewModel(
            combinedContactsRepository
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
        // Given
        val combinedContactsPreference = CombinedContactsPreference(true)
        coEvery { combinedContactsRepository.save(combinedContactsPreference) } just runs

        // When
        combinedContactsSettingViewModel.saveCombinedContactsPreference(true)

        // Then
        coVerify { combinedContactsRepository.save(combinedContactsPreference) }
    }
}
