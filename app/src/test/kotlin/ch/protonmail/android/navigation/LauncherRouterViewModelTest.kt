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
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import ch.protonmail.android.navigation.model.OnboardingEligibilityState
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LauncherRouterViewModelTest {

    private val observeOnboarding = mockk<ObserveOnboarding>()

    private val viewModel: LauncherRouterViewModel
        get() = LauncherRouterViewModel(observeOnboarding)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `should emit loading on observation start`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf()

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.Loading, awaitItem())
        }
    }

    @Test
    fun `should emit an onboarding required state when observe onboarding returns true`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(true).right())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.Required, awaitItem())
        }
    }

    @Test
    fun `should emit a non onboarding required state when observe onboarding returns false`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(false).right())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.NotRequired, awaitItem())
        }
    }

    @Test
    fun `should emit an onboarding required state when observe onboarding errors`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(PreferencesError.left())

        // When + Then
        viewModel.onboardingEligibilityState.test {
            assertEquals(OnboardingEligibilityState.Required, awaitItem())
        }
    }
}
