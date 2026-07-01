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

package ch.protonmail.android.mailcontentsearch.presentation.settings

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearContentSearchLocalData
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingSweep
import ch.protonmail.android.mailcontentsearch.presentation.settings.reducer.ContentSearchSettingsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentSearchSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserId("current-user")

    private val ownIndexingStatus = MutableStateFlow<ContentIndexingState>(ContentIndexingState.Idle)
    private val workerState = MutableStateFlow<ContentIndexingState>(ContentIndexingState.Idle)
    private val enabledFlow = MutableStateFlow(true)

    private val reducer = ContentSearchSettingsReducer()
    private val isContentSearchEnabled = mockk<IsContentSearchEnabled> {
        coEvery { this@mockk.invoke(userId) } returns true.right()
    }
    private val enableContentSearch = mockk<EnableContentSearch>()
    private val disableContentSearch = mockk<DisableContentSearch>()
    private val startContentIndexingSweep = mockk<StartContentIndexingSweep> {
        coEvery { this@mockk.invoke() } returns EnqueueIndexingResult.Scheduled
    }
    private val clearContentSearchLocalData = mockk<ClearContentSearchLocalData>()
    private val observeContentIndexingState = mockk<ObserveContentIndexingState> {
        every { this@mockk.invoke(userId) } returns workerState
    }
    private val observeContentSearchEnabled = mockk<ObserveContentSearchEnabled> {
        every { this@mockk.invoke(userId) } returns enabledFlow
    }
    private val observeContentSearchIndexingStatus = mockk<ObserveContentSearchIndexingStatus> {
        every { this@mockk.invoke(userId) } returns ownIndexingStatus
    }
    private val isContentSearchAllowedOnMobileData = mockk<IsContentSearchAllowedOnMobileData> {
        coEvery { this@mockk.invoke() } returns false
    }
    private val setAllowContentSearchOnMobileData = mockk<SetAllowContentSearchOnMobileData>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private fun viewModel() = ContentSearchSettingsViewModel(
        reducer = reducer,
        isContentSearchEnabled = isContentSearchEnabled,
        enableContentSearch = enableContentSearch,
        disableContentSearch = disableContentSearch,
        startContentIndexingSweep = startContentIndexingSweep,
        clearContentSearchLocalData = clearContentSearchLocalData,
        observeContentIndexingState = observeContentIndexingState,
        observeContentSearchEnabled = observeContentSearchEnabled,
        observeContentSearchIndexingStatus = observeContentSearchIndexingStatus,
        isContentSearchAllowedOnMobileData = isContentSearchAllowedOnMobileData,
        setAllowContentSearchOnMobileData = setAllowContentSearchOnMobileData,
        observePrimaryUserId = observePrimaryUserId
    )

    @Test
    fun `shows the percentage and active state from the rust indexing status`() = runTest {
        // Given
        ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertEquals(42.0, state.syncPercentage)
        assertTrue(state.isIndexingActive)
    }

    @Test
    fun `does not show syncing progress when content search is disabled`() = runTest {
        // Given
        enabledFlow.value = false
        ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertNull(state.syncPercentage)
        assertFalse(state.isIndexingActive)
    }

    @Test
    fun `keeps the account marked complete from rust even when the worker reports idle`() = runTest {
        // Given
        workerState.value = ContentIndexingState.Idle
        ownIndexingStatus.value = ContentIndexingState.Completed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertNull(state.syncPercentage)
        assertFalse(state.isIndexingActive)
    }

    @Test
    fun `is active while the worker is initializing even before rust reports progress`() = runTest {
        // Given
        workerState.value = ContentIndexingState.Initializing
        ownIndexingStatus.value = ContentIndexingState.Idle

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertTrue(state.isIndexingActive)
        assertNull(state.syncPercentage)
    }

    @Test
    fun `is not active when the worker is initializing but rust already reports the account complete`() = runTest {
        // Given
        workerState.value = ContentIndexingState.Initializing
        ownIndexingStatus.value = ContentIndexingState.Completed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertFalse(state.isIndexingActive)
        assertNull(state.syncPercentage)
    }

    @Test
    fun `submit ToggleContentSearch on enables content search and starts the sweep`() = runTest {
        // Given
        coEvery { enableContentSearch(userId) } returns Unit.right()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = true))

        // Then
        coVerify { enableContentSearch(userId) }
        coVerify { startContentIndexingSweep() }
    }

    @Test
    fun `submit ToggleContentSearch on does not start the sweep when enabling fails`() = runTest {
        // Given
        coEvery { enableContentSearch(userId) } returns DataError.Local.Unknown.left()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = true))

        // Then
        coVerify(exactly = 0) { startContentIndexingSweep() }
    }

    @Test
    fun `submit ToggleContentSearch off disables content search`() = runTest {
        // Given
        coEvery { disableContentSearch(userId) } returns Unit.right()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = false))

        // Then
        coVerify { disableContentSearch(userId) }
    }

    @Test
    fun `submit ToggleContentSearch on keeps previous state when enabling fails`() = runTest {
        // Given
        coEvery { isContentSearchEnabled(userId) } returns false.right()
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        coEvery { enableContentSearch(userId) } returns DataError.Local.Unknown.left()

        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = true))

        // Then
        assertFalse(viewModel.state.value.asData().isContentSearchEnabled)
    }

    @Test
    fun `submit ToggleContentSearch off keeps previous state when disabling fails`() = runTest {
        // Given
        coEvery { isContentSearchEnabled(userId) } returns true.right()
        coEvery { disableContentSearch(userId) } returns DataError.Local.Unknown.left()

        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = false))

        // Then
        assertTrue(viewModel.state.value.asData().isContentSearchEnabled)
    }

    @Test
    fun `submit ToggleAllowMobileData persists the value and reflects it in the state`() = runTest {
        // Given
        coEvery { setAllowContentSearchOnMobileData(true) } returns Unit

        // When
        val viewModel = viewModel()
        viewModel.submit(ContentSearchSettingsViewAction.ToggleAllowMobileData(enabled = true))

        // Then
        coVerify { setAllowContentSearchOnMobileData(true) }
        assertTrue(viewModel.state.value.asData().isAllowMobileDataEnabled)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `submit ToggleAllowMobileData reschedules the sweep after the debounce while enabled`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            coEvery { setAllowContentSearchOnMobileData(true) } returns Unit
            val viewModel = viewModel()

            // When
            viewModel.submit(ContentSearchSettingsViewAction.ToggleAllowMobileData(enabled = true))
            advanceUntilIdle()

            // Then
            coVerify { startContentIndexingSweep() }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `submit ToggleAllowMobileData does not reschedule the sweep when content search is disabled`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            coEvery { isContentSearchEnabled(userId) } returns false.right()
            enabledFlow.value = false
            coEvery { setAllowContentSearchOnMobileData(true) } returns Unit
            val viewModel = viewModel()

            // When
            viewModel.submit(ContentSearchSettingsViewAction.ToggleAllowMobileData(enabled = true))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { startContentIndexingSweep() }
        }

    @Test
    fun `submit ClearLocalData disables content search and clears the local data`() = runTest {
        // Given
        coEvery { disableContentSearch(userId) } returns Unit.right()
        coEvery { clearContentSearchLocalData(userId) } returns Unit.right()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ClearLocalData)

        // Then
        coVerify { disableContentSearch(userId) }
        coVerify { clearContentSearchLocalData(userId) }
    }

    @Test
    fun `submit ClearLocalData does not clear local data when disabling content search fails`() = runTest {
        // Given
        coEvery { disableContentSearch(userId) } returns DataError.Local.Unknown.left()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ClearLocalData)

        // Then
        coVerify { disableContentSearch(userId) }
        coVerify(exactly = 0) { clearContentSearchLocalData(userId) }
    }

    private fun ContentSearchSettingsState.asData(): ContentSearchSettingsState.Data {
        assertTrue(this is ContentSearchSettingsState.Data, "Expected WithData, was $this")
        return this
    }
}
