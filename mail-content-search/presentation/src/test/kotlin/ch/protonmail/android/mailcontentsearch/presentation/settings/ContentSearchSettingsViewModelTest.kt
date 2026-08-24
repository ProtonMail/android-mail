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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartOutcome
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearContentSearchLocalData
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.reducer.ContentSearchSettingsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.time.Duration.Companion.milliseconds

internal class ContentSearchSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserId("current-user")

    private val ownIndexingStatus = MutableStateFlow<ContentIndexingState>(ContentIndexingState.Idle)
    private val enabledFlow = MutableStateFlow(true)

    private val reducer = ContentSearchSettingsReducer()
    private val isContentSearchEnabled = mockk<IsContentSearchEnabled> {
        coEvery { this@mockk.invoke(userId) } returns true.right()
    }
    private val enableContentSearch = mockk<EnableContentSearch>()
    private val disableContentSearch = mockk<DisableContentSearch>()
    private val startContentIndexingForUser = mockk<StartContentIndexingForUser> {
        coEvery { this@mockk.invoke(userId) } returns ContentIndexingStartOutcome.Started.right()
    }
    private val isContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled> {
        coEvery { this@mockk.invoke(any()) } returns true
    }
    private val clearContentSearchLocalData = mockk<ClearContentSearchLocalData>()
    private val getContentSearchIndexingStatus = mockk<GetContentSearchIndexingStatus> {
        coEvery { this@mockk.invoke(userId) } returns ContentIndexingState.Idle
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
        startContentIndexingForUser = startContentIndexingForUser,
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
        clearContentSearchLocalData = clearContentSearchLocalData,
        getContentSearchIndexingStatus = getContentSearchIndexingStatus,
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
    fun `reports a failed account, and keeps the toggle on`() = runTest {
        // Given
        ownIndexingStatus.value = ContentIndexingState.Failed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertTrue(state.isIndexingFailed)
        assertTrue(state.isContentSearchEnabled)
        assertFalse(state.isIndexingActive)
        assertNull(state.syncPercentage)
    }

    @Test
    fun `stops reporting a failure once rust starts the account again`() = runTest {
        // Given
        ownIndexingStatus.value = ContentIndexingState.Failed
        val viewModel = viewModel()

        // When
        ownIndexingStatus.value = ContentIndexingState.Initializing

        // Then
        val state = viewModel.state.value.asData()
        assertFalse(state.isIndexingFailed)
        assertTrue(state.isIndexingActive)
    }

    @Test
    fun `does not report a failure while content search is disabled`() = runTest {
        // Given
        enabledFlow.value = false
        ownIndexingStatus.value = ContentIndexingState.Failed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertFalse(state.isIndexingFailed)
    }

    @Test
    fun `submit RetryIndexing hands the account back to the orchestrator`() = runTest {
        // Given
        ownIndexingStatus.value = ContentIndexingState.Failed

        // When
        viewModel().submit(ContentSearchSettingsViewAction.RetryIndexing)

        // Then
        coVerify { startContentIndexingForUser(userId) }
        coVerify(exactly = 0) { enableContentSearch(userId) }
    }

    @Test
    fun `clears the failure when the retry finds nothing left to index`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns
            ContentIndexingStartOutcome.AlreadyCompleted.right()
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.asData().isIndexingFailed)

        // When
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Completed
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.asData().isIndexingFailed)
    }

    @Test
    fun `marks the retry as pending until rust answers`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } coAnswers {
            delay(RustRoundTripMillis.milliseconds)
            ContentIndexingStartOutcome.Started.right()
        }
        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)

        // Then
        assertTrue(viewModel.state.value.asData().isRetryingIndexing)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.asData().isRetryingIndexing)
    }

    @Test
    fun `clears the failure as soon as rust takes the account back`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns ContentIndexingStartOutcome.Started.right()
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.asData().isIndexingFailed)

        // When
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value.asData()
        assertFalse(state.isIndexingFailed)
        assertTrue(state.isIndexingActive)
        assertFalse(state.isRetryingIndexing)
    }

    @Test
    fun `clears the failure when rust was already indexing the account`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns
            ContentIndexingStartOutcome.AlreadyRunning.right()
        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value.asData()
        assertFalse(state.isIndexingFailed)
        assertTrue(state.isIndexingActive)
    }

    @Test
    fun `lets a later progress event overrule an accepted retry`() = runTest {
        // Given
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns ContentIndexingStartOutcome.Started.right()
        val viewModel = viewModel()
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // When
        ownIndexingStatus.value = ContentIndexingState.Failed

        // Then
        val state = viewModel.state.value.asData()
        assertTrue(state.isIndexingFailed)
        assertFalse(state.isIndexingActive)
    }

    @Test
    fun `keeps the failure and reports an error when the retry is refused`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns ContentIndexingStartOutcome.Refused.right()
        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value.asData()
        assertTrue(state.isIndexingFailed)
        assertFalse(state.isRetryingIndexing)
        assertEquals(RetryErrorMessage, state.updateErrorEffect.consume())
    }

    @Test
    fun `reports an error when the retry never reaches rust`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed
        coEvery { startContentIndexingForUser(userId) } returns DataError.Local.Unknown.left()
        val viewModel = viewModel()

        // When
        viewModel.submit(ContentSearchSettingsViewAction.RetryIndexing)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value.asData()
        assertTrue(state.isIndexingFailed)
        assertFalse(state.isRetryingIndexing)
        assertEquals(RetryErrorMessage, state.updateErrorEffect.consume())
    }

    @Test
    fun `does not read the failure rust kept from before the account was disabled`() = runTest {
        // Given
        coEvery { isContentSearchEnabled(userId) } returns false.right()
        enabledFlow.value = false
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertFalse(state.isIndexingFailed)
        coVerify(exactly = 0) { getContentSearchIndexingStatus(userId) }
    }

    @Test
    fun `asks rust for the account state when the screen opens`() = runTest {
        // Given
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()
        coEvery { getContentSearchIndexingStatus(userId) } returns ContentIndexingState.Failed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertTrue(state.isIndexingFailed)
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
    fun `is not active when rust reports the account complete`() = runTest {
        // Given
        ownIndexingStatus.value = ContentIndexingState.Completed

        // When
        val state = viewModel().state.value.asData()

        // Then
        assertNull(state.syncPercentage)
        assertFalse(state.isIndexingActive)
    }

    @Test
    fun `is active while rust reports Initializing even before it reports progress`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            ownIndexingStatus.value = ContentIndexingState.Initializing

            // When
            val viewModel = viewModel()
            advanceUntilIdle()
            val state = viewModel.state.value.asData()

            // Then
            assertTrue(state.isIndexingActive)
            assertNull(state.syncPercentage)
        }

    @Test
    fun `surfaces the wait for an unmetered connection instead of a stalled percentage`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)
            val viewModel = viewModel()
            advanceUntilIdle()

            // When - the user drops off Wi-Fi with mobile data indexing turned off.
            ownIndexingStatus.value = ContentIndexingState.WaitingForUnmeteredConnection
            advanceUntilIdle()
            val state = viewModel.state.value.asData()

            // Then
            assertTrue(state.isWaitingForUnmeteredConnection)
            assertTrue(state.isIndexingActive)
            assertNull(state.syncPercentage)
        }

    @Test
    fun `clears the wait for an unmetered connection once rust resumes`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            ownIndexingStatus.value = ContentIndexingState.WaitingForUnmeteredConnection
            val viewModel = viewModel()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.asData().isWaitingForUnmeteredConnection)

            // When - Rust re-emits the last progress as soon as the guard is lifted.
            ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)
            advanceUntilIdle()
            val state = viewModel.state.value.asData()

            // Then
            assertFalse(state.isWaitingForUnmeteredConnection)
            assertEquals(42.0, state.syncPercentage)
        }

    @Test
    fun `blanks the percentage immediately when content search is disabled`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(42.0, viewModel.state.value.asData().syncPercentage)

            // When
            enabledFlow.value = false

            // Then
            assertNull(viewModel.state.value.asData().syncPercentage)
            assertFalse(viewModel.state.value.asData().isIndexingActive)
        }

    @Test
    fun `clears the percentage as soon as rust stops reporting progress`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            ownIndexingStatus.value = ContentIndexingState.Running(percentage = 42.0)
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(42.0, viewModel.state.value.asData().syncPercentage)

            // When
            ownIndexingStatus.value = ContentIndexingState.Cancelled
            advanceUntilIdle()

            // Then
            assertNull(viewModel.state.value.asData().syncPercentage)
        }

    @Test
    fun `does not replay the previous percentage when content search is re-enabled on the same screen`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            every { observeContentSearchIndexingStatus(userId) } returnsMany listOf(
                flowOf(ContentIndexingState.Running(percentage = 42.0)),
                flowOf(ContentIndexingState.Idle)
            )
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(42.0, viewModel.state.value.asData().syncPercentage)

            // When
            enabledFlow.value = false
            advanceUntilIdle()
            enabledFlow.value = true
            advanceUntilIdle()

            // Then
            assertNull(viewModel.state.value.asData().syncPercentage)
        }

    @Test
    fun `shows progress again after resetting local data on a previously completed account`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given
            coEvery { disableContentSearch(userId) } returns Unit.right()
            coEvery { clearContentSearchLocalData(userId) } returns Unit.right()
            ownIndexingStatus.value = ContentIndexingState.Completed
            val viewModel = viewModel()
            advanceUntilIdle()
            assertFalse(viewModel.state.value.asData().isIndexingActive)

            // When
            viewModel.submit(ContentSearchSettingsViewAction.ClearLocalData)
            advanceUntilIdle()
            enabledFlow.value = false // disabling content search is part of clearing the data
            ownIndexingStatus.value = ContentIndexingState.Idle
            advanceUntilIdle()
            enabledFlow.value = true
            ownIndexingStatus.value = ContentIndexingState.Initializing
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.state.value.asData().isIndexingActive)
        }

    @Test
    fun `submit ToggleContentSearch on enables content search and hands the account to the orchestrator`() = runTest {
        // Given
        coEvery { enableContentSearch(userId) } returns Unit.right()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = true))

        // Then
        coVerify { enableContentSearch(userId) }
        coVerify { startContentIndexingForUser(userId) }
    }

    @Test
    fun `submit ToggleContentSearch on does not start indexing when enabling fails`() = runTest {
        // Given
        coEvery { enableContentSearch(userId) } returns DataError.Local.Unknown.left()

        // When
        viewModel().submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled = true))

        // Then
        coVerify(exactly = 0) { startContentIndexingForUser(any()) }
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `submit ToggleAllowMobileData stores the value without restarting indexing`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Given - Rust owns the metered guard and pauses or resumes its own queue.
            coEvery { setAllowContentSearchOnMobileData(true) } returns Unit
            val viewModel = viewModel()

            // When
            viewModel.submit(ContentSearchSettingsViewAction.ToggleAllowMobileData(enabled = true))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { setAllowContentSearchOnMobileData(true) }
            coVerify(exactly = 0) { startContentIndexingForUser(any()) }
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

    private companion object {

        const val RustRoundTripMillis = 100L

        val RetryErrorMessage = TextUiModel.TextRes(R.string.mail_settings_content_search_retry_error)
    }
}
