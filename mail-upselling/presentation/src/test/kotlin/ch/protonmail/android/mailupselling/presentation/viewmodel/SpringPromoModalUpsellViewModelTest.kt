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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.repository.SpringPromoRepository
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.model.springsale.SpringPromoModalState
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.SpringPromoModalTrigger
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.awaits
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SpringPromoModalUpsellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility>()

    private val springPromoRepository = mockk<SpringPromoRepository>()
    private val springPromoModalTrigger = mockk<SpringPromoModalTrigger>()

    private fun viewModel() = SpringPromoModalUpsellViewModel(
        observeUpsellingVisibility,
        springPromoRepository,
        springPromoModalTrigger
    )

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should return not required when visibility is not compatible`() = runTest {
        // Given
        every {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(UpsellingVisibility.Normal)

        // When + Then
        viewModel().state.test {
            assertEquals(SpringPromoModalState.NotRequired, awaitItem())
        }
    }

    @Test
    fun `should return not required when there is no spring promo phase ongoing`() = runTest {
        // Given
        every {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(UpsellingVisibility.Promotional.SpringPromo.Wave1)
        every { springPromoModalTrigger.observe() } returns flowOf(SpringPromoPhase.None)

        // When + Then
        viewModel().state.test {
            assertEquals(SpringPromoModalState.NotRequired, awaitItem())
        }
    }

    @Test
    fun `should return a trigger for w1 when w1 phase is ongoing`() = runTest {
        // Given
        val expected = UpsellingVisibility.Promotional.SpringPromo.Wave1
        every {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(expected)
        every { springPromoModalTrigger.observe() } returns flowOf(SpringPromoPhase.Active.Wave1)

        // When + Then
        viewModel().state.test {
            assertEquals(SpringPromoModalState.Show(expected), awaitItem())
        }
    }

    @Test
    fun `should return a trigger for w2 when w2 phase is ongoing`() = runTest {
        // Given
        val expected = UpsellingVisibility.Promotional.SpringPromo.Wave2
        every {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(expected)
        every { springPromoModalTrigger.observe() } returns flowOf(SpringPromoPhase.Active.Wave2)

        // When + Then
        viewModel().state.test {
            assertEquals(SpringPromoModalState.Show(expected), awaitItem())
        }
    }

    @Test
    fun `should call the UC to save the modal timestamp`() = runTest {
        // Given
        val phase = SpringPromoPhase.Active.Wave2
        val expected = UpsellingVisibility.Promotional.SpringPromo.Wave2
        every {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(expected)

        coEvery { springPromoRepository.saveSeen(phase) } just awaits

        // When
        viewModel().saveModalSeenTimestamp(UpsellingVisibility.Promotional.SpringPromo.Wave2)

        // Then
        coVerify { springPromoRepository.saveSeen(phase) }
    }
}
