/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class SyncOnboardingFinishedToSdkTest {

    private val observeOnboarding = mockk<ObserveOnboarding>()
    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository> {
        coEvery { recordOnboardingFinished(userId) } returns Unit.right()
    }
    private val syncOnboardingFinishedToSdk = SyncOnboardingFinishedToSdk(observeOnboarding, byoeGuidanceRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the onboarding is done and not recorded, when syncing, then it is recorded`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(display = false).right())
        coEvery { byoeGuidanceRepository.isOnboardingFinishedRecorded(userId) } returns false.right()

        // When
        syncOnboardingFinishedToSdk(userId)

        // Then
        coVerify(exactly = 1) { byoeGuidanceRepository.recordOnboardingFinished(userId) }
    }

    @Test
    fun `given the onboarding is still to show, when syncing, then nothing is recorded`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(display = true).right())

        // When
        syncOnboardingFinishedToSdk(userId)

        // Then
        coVerify(exactly = 0) { byoeGuidanceRepository.recordOnboardingFinished(any()) }
    }

    @Test
    fun `given the onboarding is already recorded, when syncing, then it is not recorded again`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(display = false).right())
        coEvery { byoeGuidanceRepository.isOnboardingFinishedRecorded(userId) } returns true.right()

        // When
        syncOnboardingFinishedToSdk(userId)

        // Then
        coVerify(exactly = 0) { byoeGuidanceRepository.recordOnboardingFinished(any()) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
