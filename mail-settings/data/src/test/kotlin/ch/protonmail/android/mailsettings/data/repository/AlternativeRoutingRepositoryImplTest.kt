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

package ch.protonmail.android.mailsettings.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.data.repository.local.AlternativeRoutingLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternativeRoutingRepositoryImplTest {

    private val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true).right()
    private val alternativeRoutingPreferenceFlow = flowOf(alternativeRoutingPreference)

    private val alternativeRoutingLocalDataSource: AlternativeRoutingLocalDataSource = mockk {
        every { observe() } returns alternativeRoutingPreferenceFlow
        coEvery { save(any()) } returns Unit.right()
    }

    private val alternativeRoutingRepository = AlternativeRoutingRepositoryImpl(alternativeRoutingLocalDataSource)

    @Test
    fun `returns value from the local data source`() = runTest {
        // When
        alternativeRoutingRepository.observe().test {
            // Then
            assertEquals(AlternativeRoutingPreference(isEnabled = true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `calls the local data source save method with the correct preference`() = runTest {
        // Given
        val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true)

        // When
        alternativeRoutingRepository.save(alternativeRoutingPreference)

        // Then
        coVerify { alternativeRoutingLocalDataSource.save(alternativeRoutingPreference) }
    }
}
