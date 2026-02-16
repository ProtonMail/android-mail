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

package ch.protonmail.android.navigation

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailonboarding.domain.model.OnboardingEligibilityState
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import ch.protonmail.android.mailonboarding.domain.usecase.SaveOnboarding
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingStepAction
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingStepViewModel
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnboardingStepViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saveOnboarding = mockk<SaveOnboarding>(relaxed = true)
    private val observeOnboarding = mockk<ObserveOnboarding>()

    private val viewModel by lazy {
        OnboardingStepViewModel(saveOnboarding, observeOnboarding)
    }

    @Test
    fun `should call save onboarding when marking onboarding as completed`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(false).right())

        // When
        viewModel.submit(OnboardingStepAction.MarkOnboardingComplete)

        // Then
        coVerify(exactly = 1) { saveOnboarding(false) }
        confirmVerified(saveOnboarding)
    }

    @Test
    fun `should emit onboarding required state when observe onboarding returns true`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(true).right())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.Required, awaitItem())
        }
    }

    @Test
    fun `should emit onboarding not required state when observe onboarding returns false`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(false).right())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.NotRequired, awaitItem())
        }
    }

    @Test
    fun `should emit onboarding required state when observe onboarding errors`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(PreferencesError.left())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.Required, awaitItem())
        }
    }
}
