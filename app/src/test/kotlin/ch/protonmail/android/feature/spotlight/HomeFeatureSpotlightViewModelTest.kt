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
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.domain.usecase.IsRecentAppInstall
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveIsBusinessUser
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
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
    private val mockObserveIsBusinessUser = mockk<ObserveIsBusinessUser> {
        every { this@mockk.invoke() } returns flowOf(false.right())
    }
    private val mockObservePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserId("user-id"))
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
    fun `should emit Show with B2C user type when preference is show and user is not a business account`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())
        every { mockObserveIsBusinessUser() } returns flowOf(false.right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Show(SpotlightUserType.B2C), awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should emit Show with B2B user type when preference is show and user is a business account`() = runTest {
        // Given
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returns true
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())
        every { mockObserveIsBusinessUser() } returns flowOf(true.right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Show(SpotlightUserType.B2B), awaitItem())
        }
    }

    @Test
    fun `should not resolve user type when preference is hide`() = runTest {
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
        coVerify(exactly = 0) { mockObserveIsBusinessUser() }
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
        every { mockObserveIsBusinessUser() } returns flowOf(false.right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Show(SpotlightUserType.B2C), awaitItem())
        }
        coVerify(exactly = 0) { mockMarkFeatureSpotlightSeen() }
    }

    @Test
    fun `should re-evaluate and emit Show when primary user changes to one with the flags enabled`() = runTest {
        // Given: first user has the category view flag disabled, the second has flags enabled.
        // Drive the primary user emissions manually so we can assert the full Hide -> Show sequence
        // rather than only the final (conflated) value, which would not prove re-evaluation happened.
        val firstUser = UserId("first-user")
        val secondUser = UserId("second-user")
        val primaryUserId = MutableStateFlow<UserId?>(firstUser)
        every { mockObservePrimaryUserId() } returns primaryUserId
        coEvery { mockFeatureFlag.get() } returns true
        coEvery { mockCategoryViewFlag.get() } returnsMany listOf(false, true)
        every { mockIsRecentAppInstall() } returns false
        every { mockObserveFeatureSpotlightDisplay() } returns flowOf(FeatureSpotlightDisplay(show = true).right())
        every { mockObserveIsBusinessUser() } returns flowOf(false.right())

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(FeatureSpotlightState.Hide, awaitItem())
            primaryUserId.emit(secondUser)
            assertEquals(FeatureSpotlightState.Show(SpotlightUserType.B2C), awaitItem())
        }
    }

    private fun buildViewModel() = HomeFeatureSpotlightViewModel(
        observeFeatureSpotlightDisplay = mockObserveFeatureSpotlightDisplay,
        isEnabled = mockFeatureFlag,
        categoryViewEnabled = mockCategoryViewFlag,
        isRecentAppInstall = mockIsRecentAppInstall,
        markFeatureSpotlightSeen = mockMarkFeatureSpotlightSeen,
        observeIsBusinessUser = mockObserveIsBusinessUser,
        observePrimaryUserId = mockObservePrimaryUserId
    )
}
