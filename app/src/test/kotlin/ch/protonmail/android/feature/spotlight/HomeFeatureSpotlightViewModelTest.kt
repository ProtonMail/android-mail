/*
 * Copyright (c) 2025 Proton Technologies AG
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
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.domain.usecase.IsRecentAppInstall
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeFeatureSpotlightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockFeatureFlag = mockk<FeatureFlag<Boolean>>()
    private val mockCategoryViewFlag = mockk<FeatureFlag<Boolean>>()
    private val mockObserveFeatureSpotlightDisplay = mockk<ObserveFeatureSpotlightDisplay>()
    private val mockIsRecentAppInstall = mockk<IsRecentAppInstall>()
    private val mockMarkFeatureSpotlightSeen = mockk<MarkFeatureSpotlightSeen> {
        coEvery { this@mockk.invoke() } returns Unit.right()
    }

    @Test
    fun `should emit Hide when feature spotlight flag is disabled`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns false
        coEvery { mockCategoryViewFlag.get() } returns true

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should emit Hide when category view flag is disabled`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns false

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should emit Show when feature flag is enabled and preference is show`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Show, awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should emit Hide when feature flag is enabled and preference is hide`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = false).right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when feature flag is enabled and preference returns error`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(PreferencesError.left())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide and mark seen when feature flag is enabled and app is recent install`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns true

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
        }
        coVerify(exactly = 1) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should observe spotlight display when feature flag enabled and not recent install`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Show, awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    private fun buildViewModel() = HomeFeatureSpotlightViewModel(
        observeFeatureSpotlightDisplay = mockObserveFeatureSpotlightDisplay,
        isEnabled = mockFeatureFlag,
        categoryViewEnabled = mockCategoryViewFlag,
        isRecentAppInstall = mockIsRecentAppInstall,
        markFeatureSpotlightSeen = mockMarkFeatureSpotlightSeen
    )
}
