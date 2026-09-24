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

import app.cash.turbine.test
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchPromptDismissed
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import arrow.core.right
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
internal class ContentSearchSetUpPromptViewModelTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()

    private val observePrimaryUserId = mockk<ObservePrimaryUserIdWithValidSession> {
        every { this@mockk() } returns flowOf(userId)
    }
    private val observeContentSearchEnabled = mockk<ObserveContentSearchEnabled>()
    private val observeContentSearchPromptDismissed = mockk<ObserveContentSearchPromptDismissed> {
        every { this@mockk(userId, ContentSearchPrompt.SetUpOffer) } returns flowOf(false)
    }
    private val dismissContentSearchPrompt = mockk<DismissContentSearchPrompt> {
        coEvery { this@mockk(userId, ContentSearchPrompt.SetUpOffer) } returns Unit.right()
    }
    private val isContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled>()

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
    fun `can set up when content search is available and off for the primary account`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().canSetUpContentSearch.test {
            // Then
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `cannot set up when content search is already on`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(true)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().canSetUpContentSearch.test {
            // Then
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `cannot set up when the feature is unavailable`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns false

        // When
        viewModel().canSetUpContentSearch.test {
            // Then
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `leaves the banner undecided until the account's answer has been read`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer) } returns emptyFlow()
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().showSetUpBanner.test {
            // Then
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `shows the banner when the account has not answered the offer yet`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().showSetUpBanner.test {
            // Then
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `hides the banner once the account has answered the offer`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer) } returns flowOf(true)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().showSetUpBanner.test {
            // Then
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `hides the banner when content search is already on`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(true)
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().showSetUpBanner.test {
            // Then
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `records the answer for the primary account when the banner is dismissed`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().dismissSetUpBanner()

        // Then
        coVerify { dismissContentSearchPrompt(userId, ContentSearchPrompt.SetUpOffer) }
    }

    @Test
    fun `hides the banner without recording an answer when it is set aside`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        every { observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true
        val viewModel = viewModel()

        viewModel.showSetUpBanner.test {
            assertEquals(true, awaitItem())

            // When
            viewModel.setAsideSetUpBanner()

            // Then
            assertEquals(false, awaitItem())
        }
        coVerify(exactly = 0) { dismissContentSearchPrompt(any(), any()) }
    }

    @Test
    fun `records the answer for the primary account when set-up is declined`() = runTest(dispatcher) {
        // Given
        every { observeContentSearchEnabled(userId) } returns flowOf(false)
        coEvery { isContentSearchFeatureEnabled() } returns true

        // When
        viewModel().declineSetUp()

        // Then
        coVerify(exactly = 1) { dismissContentSearchPrompt(userId, ContentSearchPrompt.SetUpOffer) }
    }

    private fun viewModel() = ContentSearchSetUpPromptViewModel(
        observePrimaryUserId = observePrimaryUserId,
        observeContentSearchEnabled = observeContentSearchEnabled,
        observeContentSearchPromptDismissed = observeContentSearchPromptDismissed,
        dismissContentSearchPrompt = dismissContentSearchPrompt,
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled
    )
}
