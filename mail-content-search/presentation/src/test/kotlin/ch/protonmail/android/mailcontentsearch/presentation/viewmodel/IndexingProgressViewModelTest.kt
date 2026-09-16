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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchPromptDismissed
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import arrow.core.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class IndexingProgressViewModelTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()

    // Comfortably past the ViewModel's own deadline for reading the indexing status.
    private val statusReadTimeout = 3.seconds

    private val indexingStatus = MutableStateFlow<ContentIndexingState>(ContentIndexingState.Idle)

    private val observePrimaryUserId = mockk<ObservePrimaryUserIdWithValidSession> {
        every { this@mockk() } returns flowOf(userId)
    }
    private val isContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled> {
        coEvery { this@mockk() } returns true
    }
    private val observeContentSearchEnabled = mockk<ObserveContentSearchEnabled> {
        every { this@mockk(userId) } returns flowOf(true)
    }
    private val observeContentSearchIndexingStatus = mockk<ObserveContentSearchIndexingStatus> {
        every { this@mockk(userId) } returns indexingStatus
    }
    private val observeContentSearchPromptDismissed = mockk<ObserveContentSearchPromptDismissed> {
        every { this@mockk(userId, ContentSearchPrompt.IndexingComplete) } returns flowOf(false)
    }
    private val dismissContentSearchPrompt = mockk<DismissContentSearchPrompt> {
        coEvery { this@mockk(userId, ContentSearchPrompt.IndexingComplete) } returns Unit.right()
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `shows the running percentage, rounded down`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Running(percentage = 30.7)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(IndexingProgressState.Visible(percentage = 30), viewModel.state.value)
    }

    @Test
    fun `shows nothing while the account has no backfill running`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Completed

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(IndexingProgressState.Hidden, viewModel.state.value)
    }

    @Test
    fun `shows nothing when the user has content search switched off`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        indexingStatus.value = ContentIndexingState.Running(percentage = 30.0)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(IndexingProgressState.Hidden, viewModel.state.value)
    }

    @Test
    fun `shows nothing when content search is unavailable on this device`() = runTest(dispatcher) {
        // Given
        coEvery { isContentSearchFeatureEnabled() } returns false
        indexingStatus.value = ContentIndexingState.Running(percentage = 30.0)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(IndexingProgressState.Hidden, viewModel.state.value)
    }

    @Test
    fun `stays on screen at 100 percent once the account finishes indexing`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Running(percentage = 99.0)
        val viewModel = buildViewModel()

        // When
        indexingStatus.value = ContentIndexingState.Completed

        // Then
        assertEquals(IndexingProgressState.Visible(percentage = 100), viewModel.state.value)
    }

    @Test
    fun `freezes at the last percentage while the backfill waits for an unmetered connection`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Running(percentage = 42.0)
        val viewModel = buildViewModel()

        // When
        indexingStatus.value = ContentIndexingState.WaitingForUnmeteredConnection()

        // Then
        assertEquals(IndexingProgressState.Visible(percentage = 42), viewModel.state.value)
    }

    @Test
    fun `never claims 100 percent while the backfill is still running`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Running(percentage = 99.9)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(IndexingProgressState.Visible(percentage = 99), viewModel.state.value)
    }

    @Test
    fun `goes away when the user switches content search off`() = runTest(dispatcher) {
        // Given
        val enabled = MutableStateFlow(true)
        every { observeContentSearchEnabled(userId) } returns enabled
        indexingStatus.value = ContentIndexingState.Running(percentage = 30.0)
        val viewModel = buildViewModel()

        // When
        enabled.value = false

        // Then
        assertEquals(IndexingProgressState.Hidden, viewModel.state.value)
    }

    @Test
    fun `does not carry one account's banner over to the next account`() = runTest(dispatcher) {
        // Given
        val otherUserId = UserId("user-2")
        val primaryUserId = MutableStateFlow(userId)
        val otherIndexingStatus = MutableStateFlow<ContentIndexingState>(ContentIndexingState.Completed)
        every { observePrimaryUserId() } returns primaryUserId
        every { observeContentSearchEnabled(otherUserId) } returns flowOf(true)
        every { observeContentSearchIndexingStatus(otherUserId) } returns otherIndexingStatus

        indexingStatus.value = ContentIndexingState.Running(percentage = 30.0)
        val viewModel = buildViewModel()

        // When the user switches to an account that is already fully indexed
        primaryUserId.value = otherUserId

        // Then
        assertEquals(IndexingProgressState.Hidden, viewModel.state.value)
    }

    @Test
    fun `should tell the account its indexing is done when it is indexed and has not been told`() =
        runTest(dispatcher) {
            // Given
            indexingStatus.value = ContentIndexingState.Completed

            // When
            val viewModel = buildViewModel()

            // Then
            assertEquals(true, viewModel.showIndexingComplete.value)
        }

    @Test
    fun `should not tell the account its indexing is done when it has already been told`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Completed
        every {
            observeContentSearchPromptDismissed(userId, ContentSearchPrompt.IndexingComplete)
        } returns flowOf(true)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(false, viewModel.showIndexingComplete.value)
    }

    @Test
    fun `should not tell the account its indexing is done while it is still running`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Running(percentage = 50.0)

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(false, viewModel.showIndexingComplete.value)
    }

    @Test
    fun `should not tell the account its indexing is done once it finishes under the user's eyes`() =
        runTest(dispatcher) {
            // Given
            indexingStatus.value = ContentIndexingState.Running(percentage = 99.0)
            val viewModel = buildViewModel()

            // When
            indexingStatus.value = ContentIndexingState.Completed

            // Then
            assertEquals(false, viewModel.showIndexingComplete.value)
        }

    @Test
    fun `should record the answer when the account is done being told`() = runTest(dispatcher) {
        // Given
        indexingStatus.value = ContentIndexingState.Completed
        val viewModel = buildViewModel()

        // When
        viewModel.dismissIndexingComplete()

        // Then
        assertEquals(false, viewModel.showIndexingComplete.value)
        coVerify { dismissContentSearchPrompt(userId, ContentSearchPrompt.IndexingComplete) }
    }

    @Test
    fun `should settle on nothing to tell when the indexing status never arrives`() = runTest(dispatcher) {
        // Given a status flow that neither emits nor completes, as the repository's does while it
        // resubscribes on a backoff.
        every { observeContentSearchIndexingStatus(userId) } returns MutableSharedFlow()

        // When
        val viewModel = buildViewModel()
        advanceTimeBy(statusReadTimeout)

        // Then the screen has an answer to act on, rather than waiting forever.
        assertEquals(false, viewModel.showIndexingComplete.value)
    }

    @Test
    fun `should settle on nothing to tell when the indexing status is unavailable`() = runTest(dispatcher) {
        // Given the empty flow the repository returns for an account with no session available
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(false, viewModel.showIndexingComplete.value)
    }

    @Test
    fun `should not read the indexing status of an account that has already been told`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.IndexingComplete) } returns flowOf(true)
        every { observeContentSearchIndexingStatus(userId) } returns emptyFlow()

        // When
        val viewModel = buildViewModel()

        // Then the answer comes without waiting for the status at all.
        assertEquals(false, viewModel.showIndexingComplete.value)
    }

    private fun buildViewModel() = IndexingProgressViewModel(
        observePrimaryUserId = observePrimaryUserId,
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
        observeContentSearchEnabled = observeContentSearchEnabled,
        observeContentSearchIndexingStatus = observeContentSearchIndexingStatus,
        observeContentSearchPromptDismissed = observeContentSearchPromptDismissed,
        dismissContentSearchPrompt = dismissContentSearchPrompt
    )
}
