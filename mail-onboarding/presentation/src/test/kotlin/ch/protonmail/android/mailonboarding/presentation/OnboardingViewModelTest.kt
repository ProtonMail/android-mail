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

package ch.protonmail.android.mailonboarding.presentation

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeOnboardingStep
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeOnboardingTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.RecordOnboardingStepShown
import ch.protonmail.android.mailonboarding.domain.usecase.SavePendingOnboardingUpsell
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingAction
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingViewModel
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.presentation.usecase.GetUpsellingOnboardingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns flowOf(UserTestData.Primary.userId)
    }
    private val getUpsellingOnboardingVisibility = mockk<GetUpsellingOnboardingVisibility>()
    private val getByoeOnboardingTreatment = mockk<GetByoeOnboardingTreatment> {
        coEvery { this@mockk(any()) } returns null
    }
    private val recordOnboardingStepShown = mockk<RecordOnboardingStepShown>(relaxed = true)
    private val dismissByoeGuidance = mockk<DismissByoeGuidance>(relaxed = true)
    private val savePendingOnboardingUpsell = mockk<SavePendingOnboardingUpsell>(relaxed = true)

    private fun viewModel() = OnboardingViewModel(
        observePrimaryUser,
        observePrimaryUserId,
        getUpsellingOnboardingVisibility,
        getByoeOnboardingTreatment,
        recordOnboardingStepShown,
        dismissByoeGuidance,
        savePendingOnboardingUpsell
    )

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should return no upsell when user has subscription`() = runTest {
        // Given
        every { observePrimaryUser() } returns flowOf(UserTestData.Primary.copy(subscribed = 1).right())

        // When + Then
        viewModel().state.test {
            assertEquals(OnboardingState.NoUpsell(showGmailPage = false), awaitItem())
        }
    }

    @Test
    fun `should return upsell when user has no subscription and visibility is on`() = runTest {
        // Given
        givenFreeUserWithUpsell()

        // When + Then
        viewModel().state.test {
            assertEquals(OnboardingState.ToUpsell(showGmailPage = false), awaitItem())
        }
    }

    @Test
    fun `should show the Gmail page when the user is in a BYOE onboarding treatment`() = runTest {
        // Given
        givenFreeUserWithUpsell()
        coEvery { getByoeOnboardingTreatment(UserTestData.Primary.userId) } returns ByoeTreatment.Treatment1

        // When + Then
        viewModel().state.test {
            assertEquals(OnboardingState.ToUpsell(showGmailPage = true), awaitItem())
        }
    }

    @Test
    fun `should record the Gmail page only the first time it shows`() = runTest {
        // Given
        givenFreeUserWithUpsell()
        val viewModel = viewModel()

        // When
        viewModel.submit(OnboardingAction.GmailPageShown)
        viewModel.submit(OnboardingAction.GmailPageShown)

        // Then
        coVerify(exactly = 1) { recordOnboardingStepShown(UserTestData.Primary.userId, ByoeOnboardingStep.Byoe) }
    }

    @Test
    fun `should dismiss BYOE when the Gmail page is skipped`() = runTest {
        // Given
        givenFreeUserWithUpsell()

        // When
        viewModel().submit(OnboardingAction.GmailPageSkipped)

        // Then
        coVerify(exactly = 1) {
            dismissByoeGuidance(UserTestData.Primary.userId, ByoeSurface.OnboardingScreen, ByoeDismissMethod.Button)
        }
    }

    @Test
    fun `should put off the upsell when the user explores Gmail`() = runTest {
        // Given
        givenFreeUserWithUpsell()
        val viewModel = viewModel()

        // When
        viewModel.state.test {
            awaitItem()
            viewModel.submit(OnboardingAction.GmailExploreClicked)
        }

        // Then
        coVerify(exactly = 1) { savePendingOnboardingUpsell(true) }
    }

    @Test
    fun `should not put off an upsell when the user explores Gmail without one`() = runTest {
        // Given
        every { observePrimaryUser() } returns flowOf(UserTestData.Primary.copy(subscribed = 1).right())
        val viewModel = viewModel()

        // When
        viewModel.state.test {
            awaitItem()
            viewModel.submit(OnboardingAction.GmailExploreClicked)
        }

        // Then
        coVerify(exactly = 0) { savePendingOnboardingUpsell(any()) }
    }

    private fun givenFreeUserWithUpsell() {
        val user = UserTestData.Primary.copy(subscribed = 0)
        every { observePrimaryUser() } returns flowOf(user.right())
        coEvery { getUpsellingOnboardingVisibility(user.userId) } returns true
    }
}
