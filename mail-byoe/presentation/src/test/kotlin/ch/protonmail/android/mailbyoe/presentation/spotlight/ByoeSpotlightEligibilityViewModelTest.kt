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

package ch.protonmail.android.mailbyoe.presentation.spotlight

import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeSpotlightTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.RecordByoeSpotlightShown
import ch.protonmail.android.mailbyoe.domain.usecase.SyncOnboardingFinishedToSdk
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ByoeSpotlightEligibilityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns flowOf(userId)
    }
    private val getByoeSpotlightTreatment = mockk<GetByoeSpotlightTreatment>()
    private val recordByoeSpotlightShown = mockk<RecordByoeSpotlightShown>(relaxUnitFun = true)
    private val dismissByoeGuidance = mockk<DismissByoeGuidance>(relaxUnitFun = true)
    private val syncOnboardingFinishedToSdk = mockk<SyncOnboardingFinishedToSdk>(relaxUnitFun = true)

    private fun viewModel() = ByoeSpotlightEligibilityViewModel(
        observePrimaryUserId,
        getByoeSpotlightTreatment,
        recordByoeSpotlightShown,
        dismissByoeGuidance,
        syncOnboardingFinishedToSdk
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a treatment, when the app opens, then the spotlight is shown with it`() = runTest {
        // Given
        coEvery { getByoeSpotlightTreatment(userId) } returns ByoeTreatment.Treatment2

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(ByoeSpotlightEligibilityState.Show(ByoeTreatment.Treatment2), viewModel.state.value)
    }

    @Test
    fun `given the app opens, when deciding on the spotlight, then the finished onboarding is synced first`() =
        runTest {
            // Given
            coEvery { getByoeSpotlightTreatment(userId) } returns null

            // When
            viewModel()

            // Then
            coVerifyOrder {
                syncOnboardingFinishedToSdk(userId)
                getByoeSpotlightTreatment(userId)
            }
        }

    @Test
    fun `given no treatment, when the app opens, then the spotlight is hidden`() = runTest {
        // Given
        coEvery { getByoeSpotlightTreatment(userId) } returns null

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(ByoeSpotlightEligibilityState.Hide, viewModel.state.value)
    }

    @Test
    fun `given the check fails, when the app opens, then the spotlight is hidden`() = runTest {
        // Given
        coEvery { getByoeSpotlightTreatment(userId) } throws IllegalStateException("no session")

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(ByoeSpotlightEligibilityState.Hide, viewModel.state.value)
    }

    @Test
    fun `given no user signs in, when the check takes too long, then the spotlight is hidden`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            every { observePrimaryUserId() } returns emptyFlow()
            val viewModel = viewModel()

            // When
            advanceTimeBy(6_000)

            // Then
            assertEquals(ByoeSpotlightEligibilityState.Hide, viewModel.state.value)
        }

    @Test
    fun `given a check that finishes after the timeout, when it returns a treatment, then the spotlight stays hidden`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            coEvery { getByoeSpotlightTreatment(userId) } coAnswers {
                delay(7_000)
                ByoeTreatment.Treatment1
            }
            val viewModel = viewModel()

            // When
            advanceTimeBy(8_000)

            // Then
            assertEquals(ByoeSpotlightEligibilityState.Hide, viewModel.state.value)
        }

    @Test
    fun `given the spotlight is shown, when it opens, then it is hidden and recorded as shown`() = runTest {
        // Given
        coEvery { getByoeSpotlightTreatment(userId) } returns ByoeTreatment.Treatment1
        val viewModel = viewModel()

        // When
        viewModel.onOpened()

        // Then
        assertEquals(ByoeSpotlightEligibilityState.Hide, viewModel.state.value)
        coVerify(exactly = 1) { recordByoeSpotlightShown(userId) }
    }

    @Test
    fun `given the spotlight, when it is swiped away, then the swipe dismissal is recorded`() = runTest {
        // Given
        coEvery { getByoeSpotlightTreatment(userId) } returns ByoeTreatment.Treatment1
        val viewModel = viewModel()

        // When
        viewModel.onDismissed(ByoeSurface.Spotlight, ByoeDismissMethod.SwipeDown)

        // Then
        coVerify(exactly = 1) { dismissByoeGuidance(userId, ByoeSurface.Spotlight, ByoeDismissMethod.SwipeDown) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
