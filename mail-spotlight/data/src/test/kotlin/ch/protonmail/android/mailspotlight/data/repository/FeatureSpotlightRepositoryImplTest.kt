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

package ch.protonmail.android.mailspotlight.data.repository

import arrow.core.right
import ch.protonmail.android.mailspotlight.data.local.FeatureSpotlightLocalDataSource
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FeatureSpotlightRepositoryImplTest {

    private val localDataSource = mockk<FeatureSpotlightLocalDataSource>()
    private val repository = FeatureSpotlightRepositoryImpl(localDataSource)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the local data says seen, when reading the What's New, then it is seen`() = runTest {
        // Given
        coEvery { localDataSource.isContentSearchWhatsNewSeen() } returns true.right()

        // When
        val result = repository.isContentSearchWhatsNewSeen()

        // Then
        assertEquals(true.right(), result)
    }
}
