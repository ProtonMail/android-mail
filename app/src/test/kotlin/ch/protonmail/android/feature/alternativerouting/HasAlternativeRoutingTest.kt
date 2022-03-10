/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.feature.alternativerouting

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HasAlternativeRoutingTest {

    private val alternativeRoutingRepository = mockk<AlternativeRoutingRepository>()

    private lateinit var hasAlternativeRouting: HasAlternativeRouting

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        hasAlternativeRouting = HasAlternativeRouting(
            alternativeRoutingRepository,
            TestScope()
        )
    }

    @Test
    fun `emits true alternative routing preference as initial state`() = runTest {
        // Given
        every { alternativeRoutingRepository.observe() } returns flowOf()
        // When
        hasAlternativeRouting.invoke().test {

            // Then
            assertEquals(AlternativeRoutingPreference(true), awaitItem())
        }
    }

    @Test
    fun `emits alternative routing preference from repository when repository emits`() = runTest {
        // Given
        every { alternativeRoutingRepository.observe() } returns flowOf(
            AlternativeRoutingPreference(false)
        )
        // When
        hasAlternativeRouting.invoke().test {
            awaitItem() // Intial state
            // Then
            assertEquals(AlternativeRoutingPreference(false), awaitItem())
        }
    }

}
