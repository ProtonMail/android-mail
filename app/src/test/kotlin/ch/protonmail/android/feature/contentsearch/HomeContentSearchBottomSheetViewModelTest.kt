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

package ch.protonmail.android.feature.contentsearch

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.HasShownContentSearchBottomSheet
import ch.protonmail.android.mailcontentsearch.domain.usecase.MarkContentSearchBottomSheetShown
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ShouldShowContentSearchBottomSheet
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeContentSearchBottomSheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserId("user-1")
    private val mockObservePrimaryUserId = mockk<ObservePrimaryUserId> {
        coEvery { this@mockk.invoke() } returns flowOf(userId)
    }
    private val mockIsContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled>()
    private val mockObserveContentSearchEnabled = mockk<ObserveContentSearchEnabled>()
    private val mockShouldShowContentSearchBottomSheet = mockk<ShouldShowContentSearchBottomSheet>()
    private val mockHasShownContentSearchBottomSheet = mockk<HasShownContentSearchBottomSheet>()
    private val mockMarkContentSearchBottomSheetShown = mockk<MarkContentSearchBottomSheetShown> {
        coEvery { this@mockk.invoke() } returns Unit.right()
    }

    @Test
    fun `should emit Hide when the feature flag is disabled`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns false
        coEvery { mockHasShownContentSearchBottomSheet() } returns false

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when the bottom sheet was already shown`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns true
        coEvery { mockHasShownContentSearchBottomSheet() } returns true

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when content search is not enabled for the user`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns true
        coEvery { mockHasShownContentSearchBottomSheet() } returns false
        coEvery { mockObserveContentSearchEnabled(userId) } returns flowOf(false)

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Hide when should-show returns false`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns true
        coEvery { mockHasShownContentSearchBottomSheet() } returns false
        coEvery { mockObserveContentSearchEnabled(userId) } returns flowOf(true)
        coEvery { mockShouldShowContentSearchBottomSheet(userId) } returns false

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `should emit Show when enabled, not yet shown and should-show returns true`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns true
        coEvery { mockHasShownContentSearchBottomSheet() } returns false
        coEvery { mockObserveContentSearchEnabled(userId) } returns flowOf(true)
        coEvery { mockShouldShowContentSearchBottomSheet(userId) } returns true

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Show, awaitItem())
        }
    }

    @Test
    fun `stops emitting Show after the sheet is marked shown so it does not re-fire on rotation`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns true
        coEvery { mockObserveContentSearchEnabled(userId) } returns flowOf(true)
        coEvery { mockShouldShowContentSearchBottomSheet(userId) } returns true
        coEvery { mockHasShownContentSearchBottomSheet() } returnsMany listOf(false, true)

        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Show, awaitItem())
            viewModel.markShown()
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `markShown persists the bottom sheet as shown`() = runTest {
        // Given
        coEvery { mockIsContentSearchFeatureEnabled(any()) } returns false
        coEvery { mockHasShownContentSearchBottomSheet() } returns false
        val viewModel = buildViewModel()

        // When
        viewModel.markShown()

        // Then
        coVerify { mockMarkContentSearchBottomSheetShown() }
    }

    private fun buildViewModel() = HomeContentSearchBottomSheetViewModel(
        observePrimaryUserId = mockObservePrimaryUserId,
        isContentSearchFeatureEnabled = mockIsContentSearchFeatureEnabled,
        observeContentSearchEnabled = mockObserveContentSearchEnabled,
        shouldShowContentSearchBottomSheet = mockShouldShowContentSearchBottomSheet,
        hasShownContentSearchBottomSheet = mockHasShownContentSearchBottomSheet,
        markContentSearchBottomSheetShown = mockMarkContentSearchBottomSheetShown
    )
}
