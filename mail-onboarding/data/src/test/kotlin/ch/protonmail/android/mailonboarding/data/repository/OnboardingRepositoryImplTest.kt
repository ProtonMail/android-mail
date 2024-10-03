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

package ch.protonmail.android.mailonboarding.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailonboarding.data.local.OnboardingLocalDataSource
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingRepositoryImplTest {

    private val onboardingPreference = OnboardingPreference(display = true).right()
    private val onboardingPreferenceFlow = flowOf(onboardingPreference)

    private val onboardingLocalDataSource: OnboardingLocalDataSource = mockk {
        every { observe() } returns onboardingPreferenceFlow
        coEvery { save(any()) } returns Unit.right()
    }

    private val onboardingRepository = OnboardingRepositoryImpl(onboardingLocalDataSource)

    @Test
    fun `returns value from the local data source`() = runTest {
        // When
        onboardingRepository.observe().test {
            // Then
            assertEquals(OnboardingPreference(display = true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `calls the local data source save method with the correct preference`() = runTest {
        // Given
        val onboardingPreference = OnboardingPreference(display = true)

        // When
        onboardingRepository.save(onboardingPreference)

        // Then
        coVerify { onboardingLocalDataSource.save(onboardingPreference) }
    }
}
