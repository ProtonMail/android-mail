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

package ch.protonmail.android.mailonboarding.domain.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.repository.OnboardingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveOnboardingTest {

    private val onboardingPreference = OnboardingPreference(display = true).right()

    private val onboardingRepository: OnboardingRepository = mockk {
        every { observe() } returns flowOf(onboardingPreference)
    }

    private val observeOnboarding = ObserveOnboarding(onboardingRepository)

    @Test
    fun `should call observe method from repository when use case is invoked`() = runTest {
        // When
        observeOnboarding().test {
            val actual = awaitItem()
            awaitComplete()

            // Then
            verify { onboardingRepository.observe() }
            assertEquals(onboardingPreference, actual)
        }
    }
}
