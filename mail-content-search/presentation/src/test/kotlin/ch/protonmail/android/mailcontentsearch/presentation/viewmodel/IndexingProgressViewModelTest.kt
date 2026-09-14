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
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class IndexingProgressViewModelTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()

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

    private fun buildViewModel() = IndexingProgressViewModel(
        observePrimaryUserId = observePrimaryUserId,
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
        observeContentSearchEnabled = observeContentSearchEnabled,
        observeContentSearchIndexingStatus = observeContentSearchIndexingStatus
    )
}
