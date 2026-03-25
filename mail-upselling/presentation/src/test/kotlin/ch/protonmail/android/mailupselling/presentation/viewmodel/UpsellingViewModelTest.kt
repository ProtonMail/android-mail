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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.ObserveMailPlusPlanUpgrades
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.presentation.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingEntryPointKey
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

internal class UpsellingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val observeMailPlusPlanUpgrades = mockk<ObserveMailPlusPlanUpgrades>()
    private val upsellingContentReducer = mockk<UpsellingContentReducer>()
    private val eventLoopRepository = mockk<EventLoopRepository>(relaxed = true)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val resetUpgradeCache = mockk<ResetPlanUpgradesCache>()
    private val appEventBroadcaster = mockk<AppEventBroadcaster>()

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @BeforeTest
    fun setup() {
        every {
            savedStateHandle.get<String>(UpsellingEntryPointKey)
        } returns Json.encodeToString<UpsellingEntryPoint>(UpsellingEntryPoint.Feature.Navbar)
        coEvery { appEventBroadcaster.emit(any()) } just runs
    }

    @Test
    fun `should return loading error when plans fetching returns an empty list`() = runTest {
        // Given
        coEvery { observeMailPlusPlanUpgrades(any()) } returns flowOf(emptyList())
        val expectedFailure = mockk<UpsellingScreenContentState.Error>()
        coEvery {
            upsellingContentReducer.newStateFrom(UpsellingScreenContentEvent.LoadingError.NoSubscriptions)
        } returns expectedFailure

        // When
        viewModel().state.test {
            assertEquals(expectedFailure, awaitItem())
        }

        coVerify {
            upsellingContentReducer.newStateFrom(UpsellingScreenContentEvent.LoadingError.NoSubscriptions)
        }
        confirmVerified(upsellingContentReducer)
    }

    @Test
    fun `should return loading error when timeout occurs waiting for non-empty plans`() = runTest {
        // Given
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flow {
            emit(emptyList())
            delay(15.seconds)
        }

        val expectedFailure = mockk<UpsellingScreenContentState.Error>()
        coEvery {
            upsellingContentReducer.newStateFrom(UpsellingScreenContentEvent.LoadingError.NoSubscriptions)
        } returns expectedFailure

        // When
        viewModel().state.test {
            awaitItem()

            advanceTimeBy(11.seconds)

            // Then
            assertEquals(expectedFailure, awaitItem())
        }
    }

    @Test
    fun `should return data state when plans fetching returns a valid list`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.Normal }
        }
        coEvery {
            upsellingContentReducer.newStateFrom(
                operation = UpsellingScreenContentEvent.DataLoaded(
                    plans = expectedList,
                    upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar
                )
            )
        } returns expectedModel

        // When
        viewModel().state.test {
            // Then
            assertEquals(expectedModel, awaitItem())
        }

        coVerify {
            upsellingContentReducer.newStateFrom(
                UpsellingScreenContentEvent.DataLoaded(
                    plans = expectedList,
                    upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar
                )
            )
        }
        confirmVerified(upsellingContentReducer)
    }

    @Test
    fun `should emit OfferReceived when variant is IntroductoryPrice`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.IntroductoryPrice }
        }
        coEvery { upsellingContentReducer.newStateFrom(any()) } returns expectedModel

        // When
        viewModel().state.test {
            assertEquals(expectedModel, awaitItem())
        }

        // Then
        coVerify(exactly = 1) { appEventBroadcaster.emit(AppEvent.OfferReceived("intro_price")) }
    }

    @Test
    fun `should emit OfferReceived when variant is BlackFriday Wave1`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.BlackFriday.Wave1 }
        }
        coEvery { upsellingContentReducer.newStateFrom(any()) } returns expectedModel

        // When
        viewModel().state.test {
            assertEquals(expectedModel, awaitItem())
        }

        // Then
        coVerify(exactly = 1) { appEventBroadcaster.emit(AppEvent.OfferReceived("black_friday_wave1")) }
    }

    @Test
    fun `should not emit OfferReceived when variant is Normal`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.Normal }
        }
        coEvery { upsellingContentReducer.newStateFrom(any()) } returns expectedModel

        // When
        viewModel().state.test {
            assertEquals(expectedModel, awaitItem())
        }

        // Then
        coVerify(exactly = 0) { appEventBroadcaster.emit(match { it is AppEvent.OfferReceived }) }
    }

    @Test
    fun `should emit OfferClicked when purchase clicked with promotional variant`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.IntroductoryPrice }
        }
        coEvery { upsellingContentReducer.newStateFrom(any()) } returns expectedModel

        // When
        val vm = viewModel()
        vm.state.test {
            assertEquals(expectedModel, awaitItem())
        }
        vm.onPurchaseClicked()

        // Then
        coVerify(exactly = 1) { appEventBroadcaster.emit(AppEvent.OfferClicked("intro_price")) }
    }

    @Test
    fun `should not emit OfferClicked when purchase clicked with normal variant`() = runTest {
        // Given
        val expectedList = listOf<ProductOfferDetail>(mockk(), mockk())
        coEvery { observeMailPlusPlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedList)

        val expectedModel = mockk<UpsellingScreenContentState.Data> {
            every { plans } returns mockk { every { variant } returns PlanUpgradeVariant.Normal }
        }
        coEvery { upsellingContentReducer.newStateFrom(any()) } returns expectedModel

        // When
        val vm = viewModel()
        vm.state.test {
            assertEquals(expectedModel, awaitItem())
        }
        vm.onPurchaseClicked()

        // Then
        coVerify(exactly = 0) { appEventBroadcaster.emit(match { it is AppEvent.OfferClicked }) }
    }

    private fun viewModel() = UpsellingViewModel(
        savedStateHandle,
        observeMailPlusPlanUpgrades,
        upsellingContentReducer,
        eventLoopRepository,
        observePrimaryUserId,
        resetUpgradeCache,
        appEventBroadcaster
    )
}
