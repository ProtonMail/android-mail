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

package ch.protonmail.android.mailonboarding.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingOnboardingVisibility
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnboardingViewModelTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val observeUpsellingOnboardingVisibility = mockk<ObserveUpsellingOnboardingVisibility>()

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `should emit loading when first observed`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.paidUser)

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            assertEquals(OnboardingState.Loading, awaitItem())
        }
    }

    @Test
    fun `should emit no upsell if user is paid (non mail)`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.paidUser)

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Loading
            assertEquals(OnboardingState.NoUpsell, awaitItem())
        }
    }

    @Test
    fun `should emit no upsell if user is paid (mail)`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.paidMailUser)

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Loading
            assertEquals(OnboardingState.NoUpsell, awaitItem())
        }
    }

    @Test
    fun `should emit no upsell if user is free but created today`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.freeUser)
        every { getAccountAgeInDays(UserTestData.freeUser).days } returns 0

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Loading
            assertEquals(OnboardingState.NoUpsell, awaitItem())
        }
    }

    @Test
    fun `should emit no upsell if user is free, not created today but not eligible`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.freeUser)
        every { getAccountAgeInDays(UserTestData.freeUser).days } returns 1
        every { observeUpsellingOnboardingVisibility() } returns flowOf(false)

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Loading
            assertEquals(OnboardingState.NoUpsell, awaitItem())
        }
    }

    @Test
    fun `should emit to upsell if user is free, not created today and eligible`() = runTest {
        every { observePrimaryUser() } returns flowOf(UserTestData.freeUser)
        every { getAccountAgeInDays(UserTestData.freeUser).days } returns 1
        every { observeUpsellingOnboardingVisibility() } returns flowOf(true)

        // Given
        val viewModel = OnboardingViewModel(
            observePrimaryUser,
            getAccountAgeInDays,
            observeUpsellingOnboardingVisibility
        )

        // When + Then
        viewModel.state.test {
            skipItems(1) // Loading
            assertEquals(OnboardingState.ToUpsell, awaitItem())
        }
    }
}
