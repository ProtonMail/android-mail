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
import arrow.core.right
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FeatureSpotlightLocalDataSourceImplTest {

    private val prefKey = intPreferencesKey(FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY)

    private val dataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<FeatureSpotlightDataStoreProvider> {
        every { featureSpotlightDataStore } returns dataStore
    }
    private val localDataSource = FeatureSpotlightLocalDataSourceImpl(dataStoreProvider)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the content search version was seen, when reading it, then the What's New is seen`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(preferencesOf(prefKey to FeatureSpotlightVersions.CONTENT_SEARCH))

        // When
        val result = localDataSource.isContentSearchWhatsNewSeen()

        // Then
        assertEquals(true.right(), result)
    }

    @Test
    fun `given an older version was seen, when reading it, then the What's New is not seen`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(preferencesOf(prefKey to FeatureSpotlightVersions.CATEGORY_VIEW))

        // When
        val result = localDataSource.isContentSearchWhatsNewSeen()

        // Then
        assertEquals(false.right(), result)
    }

    @Test
    fun `given nothing was stored, when reading it, then the What's New is not seen`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(preferencesOf())

        // When
        val result = localDataSource.isContentSearchWhatsNewSeen()

        // Then
        assertEquals(false.right(), result)
    }
}
