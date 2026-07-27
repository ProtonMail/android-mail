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

package ch.protonmail.android.feature.spotlight

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcategory.domain.model.CategorySpotlightType
import ch.protonmail.android.mailcategory.domain.usecase.MarkCategorySpotlightSeen
import ch.protonmail.android.mailcategory.domain.usecase.ObserveCategorySpotlightSeen
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsession.domain.usecase.IsCategoryViewEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeRecategoriseSpotlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockCategoryViewFlag = mockk<IsCategoryViewEnabled>()
    private val mockObserveFeatureSpotlightDisplay = mockk<ObserveFeatureSpotlightDisplay>()
    private val mockObservePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserId("user-id"))
    }
    private val mockObserveCategorySpotlightSeen = mockk<ObserveCategorySpotlightSeen>()
    private val mockMarkCategorySpotlightSeen = mockk<MarkCategorySpotlightSeen> {
        coEvery { this@mockk.invoke(any()) } returns Unit.right()
    }

    @Test
    fun `should emit Hide when category view flag is disabled`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns false

        // When / Then
        buildViewModel().state.test {
            assertEquals(RecategoriseSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Show when feature spotlight is seen and personalise is not seen`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns true
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = false).right())
        every {
            mockObserveCategorySpotlightSeen(CategorySpotlightType.Personalise)
        } returns flowOf(false.right())

        // When / Then
        buildViewModel().state.test {
            assertEquals(RecategoriseSpotlightState.Show, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when feature spotlight has not been seen yet`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns true
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())
        every {
            mockObserveCategorySpotlightSeen(CategorySpotlightType.Personalise)
        } returns flowOf(false.right())

        // When / Then
        buildViewModel().state.test {
            assertEquals(RecategoriseSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when personalise has already been seen`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns true
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = false).right())
        every {
            mockObserveCategorySpotlightSeen(CategorySpotlightType.Personalise)
        } returns flowOf(true.right())

        // When / Then
        buildViewModel().state.test {
            assertEquals(RecategoriseSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when feature spotlight preference returns error`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns true
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(PreferencesError.left())
        every {
            mockObserveCategorySpotlightSeen(CategorySpotlightType.Personalise)
        } returns flowOf(false.right())

        // When / Then
        buildViewModel().state.test {
            assertEquals(RecategoriseSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should mark personalise seen`() = runTest {
        // Given
        coEvery { mockCategoryViewFlag(any()) } returns true
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = false).right())
        every {
            mockObserveCategorySpotlightSeen(CategorySpotlightType.Personalise)
        } returns flowOf(false.right())

        // When
        buildViewModel().markPersonaliseSeen()

        // Then
        coVerify(exactly = 1) { mockMarkCategorySpotlightSeen(CategorySpotlightType.Personalise) }
    }

    private fun buildViewModel() = HomeRecategoriseSpotlightViewModel(
        observeFeatureSpotlightDisplay = mockObserveFeatureSpotlightDisplay,
        observeCategorySpotlightSeen = mockObserveCategorySpotlightSeen,
        isCategoryViewEnabled = mockCategoryViewFlag,
        observePrimaryUserId = mockObservePrimaryUserId,
        markCategorySpotlightSeen = mockMarkCategorySpotlightSeen
    )
}
