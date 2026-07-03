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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcategory.domain.usecase.ObserveCategorySpotlightSeen
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategorySpotlightState
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveCategorySpotlightStateTest {

    private val unseenCategory = CategoryItemUiModelSample.social.copy(isActive = false, hasUnseen = true)

    private val observeCategorySpotlightSeen = mockk<ObserveCategorySpotlightSeen> {
        every { this@mockk.invoke(any()) } returns flowOf(false.right())
    }
    private val observeOnboarding = mockk<ObserveOnboarding> {
        every { this@mockk.invoke() } returns flowOf(OnboardingPreference(display = false).right())
    }

    private val observeCategorySpotlightState = ObserveCategorySpotlightState(
        observeCategorySpotlightSeen = observeCategorySpotlightSeen,
        observeOnboarding = observeOnboarding
    )

    @Test
    fun `given not seen and inactive unseen category, then unseen spotlight is shown`() = runTest {
        // Given & When
        invoke(categories = listOf(unseenCategory)).test {
            // Then
            assertEquals(CategorySpotlightState.Shown.UnseenCategory(unseenCategory), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given unseen already seen, then unseen spotlight is not shown`() = runTest {
        // Given
        every { observeCategorySpotlightSeen(CategorySpotlightType.UnseenCategory) } returns flowOf(true.right())
        every { observeCategorySpotlightSeen(CategorySpotlightType.Personalise) } returns flowOf(true.right())

        // When
        invoke(categories = listOf(unseenCategory)).test {
            // Then
            assertEquals(CategorySpotlightState.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given onboarding not completed, then spotlight is hidden even with unseen category`() = runTest {
        // Given
        every { observeOnboarding() } returns flowOf(OnboardingPreference(display = true).right())

        // When
        invoke(categories = listOf(unseenCategory)).test {
            // Then
            assertEquals(CategorySpotlightState.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given category bar not available, then spotlight is hidden`() = runTest {
        // When
        invoke(categories = null).test {
            // Then
            assertEquals(CategorySpotlightState.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given unseen consumed but personalise not seen, then personalise spotlight is shown`() = runTest {
        // Given
        every { observeCategorySpotlightSeen(CategorySpotlightType.UnseenCategory) } returns flowOf(true.right())
        every { observeCategorySpotlightSeen(CategorySpotlightType.Personalise) } returns flowOf(false.right())

        // When
        invoke(categories = listOf(unseenCategory)).test {
            // Then
            assertEquals(CategorySpotlightState.Shown.Personalise, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given both spotlights eligible, then unseen spotlight is shown`() = runTest {
        // When
        invoke(categories = listOf(unseenCategory)).test {
            // Then
            assertEquals(CategorySpotlightState.Shown.UnseenCategory(unseenCategory), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given unseen dismissed in the current session, then personalise spotlight is shown`() = runTest {
        // When
        invoke(categories = listOf(unseenCategory), unseenDismissed = flowOf(true)).test {
            // Then
            assertEquals(CategorySpotlightState.Shown.Personalise, awaitItem())
            awaitComplete()
        }
    }

    private fun invoke(
        categories: List<CategoryItemUiModel>?,
        unseenDismissed: Flow<Boolean> = flowOf(false),
        personaliseDismissed: Flow<Boolean> = flowOf(false)
    ) = observeCategorySpotlightState(
        categories = flowOf(categories),
        unseenDismissed = unseenDismissed,
        personaliseDismissed = personaliseDismissed
    )
}
