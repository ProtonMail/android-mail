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

package ch.protonmail.android.mailmailbox.data

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailmailbox.data.repository.SpotlightRepositoryImpl
import ch.protonmail.android.mailmailbox.data.repository.local.SpotlightLocalDataSource
import ch.protonmail.android.mailmailbox.domain.model.SpotlightPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SpotlightRepositoryImplTest {

    private val spotlightPreference = SpotlightPreference(display = true).right()
    private val spotlightPreferenceFlow = flowOf(spotlightPreference)

    private val spotlightLocalDataSource: SpotlightLocalDataSource = mockk {
        every { observe() } returns spotlightPreferenceFlow
        coEvery { save(any()) } returns Unit.right()
    }

    private val spotlightRepository = SpotlightRepositoryImpl(spotlightLocalDataSource)

    @Test
    fun `returns value from the local data source`() = runTest {
        // When
        spotlightRepository.observe().test {
            // Then
            Assert.assertEquals(SpotlightPreference(display = true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `calls the local data source save method with the correct preference`() = runTest {
        // Given
        val spotlightPreference = SpotlightPreference(display = true)

        // When
        spotlightRepository.save(spotlightPreference)

        // Then
        coVerify { spotlightLocalDataSource.save(spotlightPreference) }
    }
}
