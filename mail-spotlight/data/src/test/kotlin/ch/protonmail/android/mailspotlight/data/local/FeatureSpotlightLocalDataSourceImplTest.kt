/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FeatureSpotlightLocalDataSourceImplTest {

    private val prefKey = intPreferencesKey(FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY)

    private val dataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<FeatureSpotlightDataStoreProvider> {
        every { featureSpotlightDataStore } returns dataStore
    }
    private val localDataSource = FeatureSpotlightLocalDataSourceImpl(dataStoreProvider)

    @Test
    fun `observe returns shouldShow true when stored version is lower than current`() = runTest {
        // Given
        val preferences = preferencesOf(prefKey to 0)
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        localDataSource.observe().test {
            val result = awaitItem()
            assertEquals(FeatureSpotlightDisplay(show = true).right(), result)
            awaitComplete()
        }
    }

    @Test
    fun `observe returns shouldShow false when stored version equals current`() = runTest {
        // Given
        val preferences = preferencesOf(prefKey to FeatureSpotlightVersions.CATEGORY_VIEW)
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        localDataSource.observe().test {
            val result = awaitItem()
            assertEquals(FeatureSpotlightDisplay(show = false).right(), result)
            awaitComplete()
        }
    }

    @Test
    fun `observe returns shouldShow true when preference key is not set`() = runTest {
        // Given
        val emptyPreferences = preferencesOf()
        every { dataStore.data } returns flowOf(emptyPreferences)

        // When & Then
        localDataSource.observe().test {
            val result = awaitItem()
            assertEquals(FeatureSpotlightDisplay(show = true).right(), result)
            awaitComplete()
        }
    }

    @Test
    fun `save stores current spotlight version and returns success`() = runTest {
        // Given
        val updatedPreferences = preferencesOf(prefKey to FeatureSpotlightVersions.PRIVACY_BUNDLE)
        coEvery { dataStore.updateData(any()) } returns updatedPreferences

        // When
        val result = localDataSource.save()

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { dataStore.updateData(any()) }
    }
}
