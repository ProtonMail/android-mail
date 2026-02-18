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

package ch.protonmail.android.mailupselling.data.repository

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.data.local.SpringPromoLocalDataSource
import ch.protonmail.android.mailupselling.domain.model.SpringOfferSeenPreference
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SpringPromoRepositoryImplTest {

    private val dataSource = mockk<SpringPromoLocalDataSource>()

    private lateinit var repository: SpringPromoRepositoryImpl

    @BeforeTest
    fun setup() {
        repository = SpringPromoRepositoryImpl(dataSource)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should proxy the observation to the datasource (success)`() = runTest {
        // Given
        val phase = SpringPromoPhase.Active.Wave1
        val expected = SpringOfferSeenPreference(phase, 0L)
        every { dataSource.observePhaseEligibility(phase) } returns flowOf(expected.right())

        // When + Then
        repository.observePhaseEligibility(phase).test {
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should proxy the observation to the datasource (failure)`() = runTest {
        // Given
        val phase = SpringPromoPhase.Active.Wave1
        every { dataSource.observePhaseEligibility(phase) } returns flowOf(PreferencesError.left())

        // When + Then
        repository.observePhaseEligibility(phase).test {
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should proxy the save to the datasource (success)`() = runTest {
        // Given
        val phase = SpringPromoPhase.Active.Wave1
        coEvery { dataSource.saveSeen(phase) } returns Unit.right()

        // When + Then
        val actual = repository.saveSeen(phase)

        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should proxy the save to the datasource (failure)`() = runTest {
        // Given
        val phase = SpringPromoPhase.Active.Wave1
        coEvery { dataSource.saveSeen(phase) } returns PreferencesError.left()

        // When + Then
        val actual = repository.saveSeen(phase)

        assertEquals(PreferencesError.left(), actual)
    }
}
