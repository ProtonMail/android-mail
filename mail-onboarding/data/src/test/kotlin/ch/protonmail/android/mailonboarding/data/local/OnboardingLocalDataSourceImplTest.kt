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

package ch.protonmail.android.mailonboarding.data.local

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailonboarding.data.OnboardingDataStoreProvider
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val onboardingDataStoreMock = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<OnboardingDataStoreProvider> {
        every { this@mockk.onboardingDataStore } returns onboardingDataStoreMock
    }

    private val onboardingLocalDataSource = OnboardingLocalDataSourceImpl(dataStoreProvider)

    @Test
    fun `returns true when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { onboardingDataStoreMock.data } returns flowOf(preferences)

        // When
        onboardingLocalDataSource.observe().test {
            // Then
            assertEquals(OnboardingPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("shouldDisplayOnboardingPrefKey")] } returns false
        every { onboardingDataStoreMock.data } returns flowOf(preferences)

        // When
        onboardingLocalDataSource.observe().test {
            // Then
            assertEquals(OnboardingPreference(false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { onboardingDataStoreMock.data } returns flow { throw IOException() }

        // When
        onboardingLocalDataSource.observe().test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        val onboardingPreference = OnboardingPreference(display = true)
        coEvery { onboardingDataStoreMock.updateData(any()) } returns mockk()

        // When
        val result = onboardingLocalDataSource.save(onboardingPreference)

        // Then
        coVerify { onboardingDataStoreMock.updateData(any()) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val onboardingPreference = OnboardingPreference(display = true)
        coEvery { onboardingDataStoreMock.updateData(any()) } throws IOException()

        // When
        val result = onboardingLocalDataSource.save(onboardingPreference)

        // Then
        assertEquals(PreferencesError.left(), result)
    }
}
