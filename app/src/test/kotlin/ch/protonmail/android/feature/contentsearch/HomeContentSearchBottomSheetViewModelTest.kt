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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartOutcome
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
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
    private val mockIsContentSearchAllowedOnMobileData = mockk<IsContentSearchAllowedOnMobileData> {
        coEvery { this@mockk.invoke() } returns true
    }
    private val mockSetAllowContentSearchOnMobileData = mockk<SetAllowContentSearchOnMobileData>(
        relaxUnitFun = true
    )
    private val mockEnableContentSearch = mockk<EnableContentSearch> {
        coEvery { this@mockk.invoke(userId) } returns Unit.right()
    }
    private val mockStartContentIndexingForUser = mockk<StartContentIndexingForUser> {
        coEvery { this@mockk.invoke(userId) } returns ContentIndexingStartOutcome.Started.right()
    }
    private val mockDismissContentSearchPrompt = mockk<DismissContentSearchPrompt> {
        coEvery { this@mockk.invoke(userId, ContentSearchPrompt.SetUpOffer) } returns Unit.right()
    }

    @Test
    fun `should start hidden so it does not hold up the other home interstitials`() = runTest {
        // Given
        val viewModel = buildViewModel()

        // When/Then
        assertEquals(ContentSearchBottomSheetState.Hide, viewModel.state.value)
    }

    @Test
    fun `should show the sheet on an enable request`() = runTest {
        // Given
        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
            viewModel.submit(HomeContentSearchBottomSheetViewAction.RequestToEnable)
            assertEquals(ContentSearchBottomSheetState.Show, awaitItem())
        }
    }

    @Test
    fun `should stop offering the sheet once it has been dismissed`() = runTest {
        // Given
        val viewModel = buildViewModel()

        // When/Then
        viewModel.state.test {
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
            viewModel.submit(HomeContentSearchBottomSheetViewAction.RequestToEnable)
            assertEquals(ContentSearchBottomSheetState.Show, awaitItem())
            viewModel.submit(HomeContentSearchBottomSheetViewAction.Dismiss)
            assertEquals(ContentSearchBottomSheetState.Hide, awaitItem())
        }
    }

    @Test
    fun `should seed the mobile data toggle from the stored preference`() = runTest {
        // Given
        coEvery { mockIsContentSearchAllowedOnMobileData() } returns false

        // When
        val viewModel = buildViewModel()

        // Then
        viewModel.isMobileDataEnabled.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `toggling mobile data persists the new value and reflects it immediately`() = runTest {
        // Given
        coEvery { mockIsContentSearchAllowedOnMobileData() } returns true
        val viewModel = buildViewModel()

        // When
        viewModel.isMobileDataEnabled.test {
            assertEquals(true, awaitItem())
            viewModel.submit(HomeContentSearchBottomSheetViewAction.ToggleMobileData(enabled = false))

            // Then
            assertEquals(false, awaitItem())
        }
        coVerify { mockSetAllowContentSearchOnMobileData(false) }
    }

    @Test
    fun `enabling for the primary user enables the account and starts indexing`() = runTest {
        // Given
        val viewModel = buildViewModel()

        // When
        viewModel.submit(HomeContentSearchBottomSheetViewAction.EnableForPrimaryUser)

        // Then
        coVerify(exactly = 1) { mockEnableContentSearch(userId) }
        coVerify(exactly = 1) { mockStartContentIndexingForUser(userId) }
    }

    @Test
    fun `enabling for the primary user answers the search screen's set-up offer`() = runTest {
        // Given
        val viewModel = buildViewModel()

        // When
        viewModel.submit(HomeContentSearchBottomSheetViewAction.EnableForPrimaryUser)

        // Then
        coVerify(exactly = 1) { mockDismissContentSearchPrompt(userId, ContentSearchPrompt.SetUpOffer) }
    }

    @Test
    fun `failing to enable leaves the set-up offer unanswered`() = runTest {
        // Given
        coEvery { mockEnableContentSearch(userId) } returns DataError.Local.Unknown.left()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(HomeContentSearchBottomSheetViewAction.EnableForPrimaryUser)

        // Then
        coVerify(exactly = 0) { mockDismissContentSearchPrompt(any(), any()) }
    }

    private fun buildViewModel() = HomeContentSearchBottomSheetViewModel(
        observePrimaryUserId = mockObservePrimaryUserId,
        isContentSearchAllowedOnMobileData = mockIsContentSearchAllowedOnMobileData,
        setAllowContentSearchOnMobileData = mockSetAllowContentSearchOnMobileData,
        enableContentSearch = mockEnableContentSearch,
        startContentIndexingForUser = mockStartContentIndexingForUser,
        dismissContentSearchPrompt = mockDismissContentSearchPrompt
    )
}
