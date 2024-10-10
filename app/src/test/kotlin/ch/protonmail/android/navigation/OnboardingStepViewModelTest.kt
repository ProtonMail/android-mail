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

import ch.protonmail.android.mailonboarding.domain.usecase.SaveOnboarding
import ch.protonmail.android.navigation.onboarding.OnboardingStepAction
import ch.protonmail.android.navigation.onboarding.OnboardingStepViewModel
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class OnboardingStepViewModelTest {

    private val saveOnboarding = mockk<SaveOnboarding>(relaxed = true)
    private val viewModel = OnboardingStepViewModel(saveOnboarding)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should call save onboarding when marking onboarding as completed`() = runTest {
        // When
        viewModel.submit(OnboardingStepAction.MarkOnboardingComplete)

        // Then
        coVerify(exactly = 1) { saveOnboarding(false) }
        confirmVerified(saveOnboarding)
    }
}
