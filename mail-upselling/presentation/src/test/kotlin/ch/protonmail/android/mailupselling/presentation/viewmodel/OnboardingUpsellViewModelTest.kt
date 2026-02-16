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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.usecase.GetOnboardingPlanUpgrades
import ch.protonmail.android.mailupselling.domain.usecase.GetOnboardingPlansError
import ch.protonmail.android.mailupselling.presentation.OnboardingUpsellingReducer
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test

internal class OnboardingUpsellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val reducer = mockk<OnboardingUpsellingReducer>(relaxed = true)
    private val getOnboardingPlanUpgrades = mockk<GetOnboardingPlanUpgrades>()
    private val appEventBroadcaster = mockk<AppEventBroadcaster>()

    private val isUpsellEnabled = mockk<FeatureFlag<Boolean>>()

    private fun viewModel() = OnboardingUpsellViewModel(
        observePrimaryUser,
        reducer,
        getOnboardingPlanUpgrades,
        isUpsellEnabled,
        appEventBroadcaster
    )

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should emit an error when no user is found`() = runTest {
        // Given
        every { observePrimaryUser() } returns flowOf(DataError.Local.NoUserSession.left())
        coEvery { isUpsellEnabled.get() } returns false

        // When
        viewModel().state.test {
            awaitItem()
        }

        // Then
        coVerify { reducer.newStateFrom(OnboardingUpsellEvent.LoadingError.NoUserId) }
        confirmVerified(reducer)
    }

    @Test
    fun `should emit unsupported flow when FF is off`() = runTest {
        // Given
        every { observePrimaryUser() } returns flowOf(UserTestData.Primary.right())
        coEvery { isUpsellEnabled.get() } returns false

        // When
        viewModel().state.test {
            awaitItem() // Ignore, it's a mocked state
        }

        // Then
        coVerify { reducer.newStateFrom(OnboardingUpsellEvent.UnsupportedFlow.NotEnabled) }
        confirmVerified(reducer)
    }

    @Test
    fun `should emit unsupported flow when user is paid`() = runTest {
        // Given
        every { observePrimaryUser() } returns flowOf(UserTestData.Primary.copy(subscribed = 1).right())
        coEvery { isUpsellEnabled.get() } returns true

        // When
        viewModel().state.test {
            awaitItem() // Ignore, it's a mocked state
        }

        // Then
        coVerify { reducer.newStateFrom(OnboardingUpsellEvent.UnsupportedFlow.PaidUser) }
        confirmVerified(reducer)
    }

    @Test
    fun `should emit plans mismatched when product details are not fetched correctly`() = runTest {
        // Given
        val user = UserTestData.Primary.copy(subscribed = 0)
        every { observePrimaryUser() } returns flowOf(user.right())
        coEvery { getOnboardingPlanUpgrades(user.userId) } returns GetOnboardingPlansError.MismatchingPlans.left()
        coEvery { isUpsellEnabled.get() } returns true

        // When
        viewModel().state.test {
            awaitItem() // Ignore, it's a mocked state
        }

        // Then
        coVerify { reducer.newStateFrom(OnboardingUpsellEvent.UnsupportedFlow.PlansMismatch) }
        confirmVerified(reducer)
    }

    @Test
    fun `should emit data loaded when product details are fetched correctly`() = runTest {
        // Given
        val user = UserTestData.Primary.copy(subscribed = 0)
        val expectedList = listOf<ProductOfferDetail>(mockk())
        every { observePrimaryUser() } returns flowOf(user.right())
        coEvery { appEventBroadcaster.emit(any()) } just runs
        coEvery { getOnboardingPlanUpgrades(user.userId) } returns expectedList.right()
        coEvery { isUpsellEnabled.get() } returns true

        // When
        viewModel().state.test {
            awaitItem() // Ignore, it's a mocked state
        }

        // Then
        coVerify { reducer.newStateFrom(OnboardingUpsellEvent.DataLoaded(user.userId, expectedList)) }
        confirmVerified(reducer)
    }

    @Test
    fun `should broadcast subscription onboarding shown event when data loads successfully`() = runTest {
        // Given
        val user = UserTestData.Primary.copy(subscribed = 0)
        val expectedList = listOf<ProductOfferDetail>(mockk())
        every { observePrimaryUser() } returns flowOf(user.right())
        coEvery { appEventBroadcaster.emit(any()) } just runs
        coEvery { getOnboardingPlanUpgrades(user.userId) } returns expectedList.right()
        coEvery { isUpsellEnabled.get() } returns true

        // When
        viewModel().state.test {
            awaitItem()
        }

        // Then
        coVerify(exactly = 1) { appEventBroadcaster.emit(AppEvent.SubscriptionOnboardingShown) }
        confirmVerified(appEventBroadcaster)
    }
}
