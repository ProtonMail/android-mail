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

package ch.protonmail.android.mailcontentsearch.domain.handler

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ResumeContentIndexingSweep
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingSweep
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class ContentSearchAutoIndexingHandlerTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val isContentSearchEnabled = mockk<IsContentSearchEnabled> {
        coEvery { this@mockk.invoke(any()) } returns false.right()
    }
    private val settingsRepository = mockk<ContentSearchSettingsRepository> {
        coEvery { setEnabled(any(), any()) } returns Unit.right()
    }
    private val startContentIndexingSweep = mockk<StartContentIndexingSweep> {
        coEvery { this@mockk.invoke() } returns EnqueueIndexingResult.Scheduled
    }
    private val resumeContentIndexingSweep = mockk<ResumeContentIndexingSweep> {
        coEvery { this@mockk.invoke() } returns EnqueueIndexingResult.Scheduled
    }
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns emptyFlow()
    }
    private var persistedKnownUserIds: Set<UserId> = emptySet()
    private val preferencesRepository = mockk<ContentSearchPreferencesRepository> {
        coEvery { hasUserOptedOut(any()) } returns false.right()
        coEvery { markUserOptedOut(any()) } returns Unit.right()
        coEvery { clearUserOptedOut(any()) } returns Unit.right()
        coEvery { getKnownUserIds() } answers { persistedKnownUserIds.right() }
        coEvery { saveKnownUserIds(any()) } answers {
            persistedKnownUserIds = firstArg()
            Unit.right()
        }
    }

    @Test
    fun `enables a disabled account that has not opted out and starts the sweep`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenRustEnabled(UserOne, enabled = false)
        givenOptedOut(UserOne, optedOut = false)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { settingsRepository.setEnabled(UserOne, enabled = true) }
        coVerify(exactly = 1) { startContentIndexingSweep() }
    }

    @Test
    fun `does not re-enable an already-enabled account but clears a stale opt-out and resumes the sweep`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenRustEnabled(UserOne, enabled = true)
        givenOptedOut(UserOne, optedOut = true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
        coVerify(exactly = 1) { preferencesRepository.clearUserOptedOut(UserOne) }
        coVerify(exactly = 1) { startContentIndexingSweep() }
    }

    @Test
    fun `does not re-enable an account the user has opted out of`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenRustEnabled(UserOne, enabled = false)
        givenOptedOut(UserOne, optedOut = true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
        coVerify(exactly = 1) { startContentIndexingSweep() }
    }

    @Test
    fun `ignores accounts that are not ready`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne, state = AccountState.NotReady))))

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
        coVerify(exactly = 0) { startContentIndexingSweep() }
    }

    @Test
    fun `enables a newly logged-in disabled account and restarts the sweep`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne)), listOf(account(UserOne), account(UserTwo))))
        givenRustEnabled(UserOne, enabled = true)
        givenRustEnabled(UserTwo, enabled = false)
        givenOptedOut(UserTwo, optedOut = false)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { settingsRepository.setEnabled(UserTwo, enabled = true) }
        coVerify(exactly = 0) { settingsRepository.setEnabled(UserOne, any()) }
        coVerify(exactly = 2) { startContentIndexingSweep() }
    }

    @Test
    fun `restarts the sweep when an already-enabled account newly becomes ready`() = runTest {
        // Given
        givenAccounts(
            flowOf(
                listOf(account(UserOne)),
                listOf(account(UserOne), account(UserTwo))
            )
        )
        givenRustEnabled(UserOne, enabled = true)
        givenRustEnabled(UserTwo, enabled = true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
        coVerify(exactly = 2) { startContentIndexingSweep() }
    }

    @Test
    fun `clears the opt-out when an account is signed out`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne)), emptyList()))
        givenRustEnabled(UserOne, enabled = false)
        givenOptedOut(UserOne, optedOut = true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { preferencesRepository.clearUserOptedOut(UserOne) }
    }

    @Test
    fun `clears a stale opt-out on a fresh launch when the sign-out happened while the process was dead`() = runTest {
        // Given
        persistedKnownUserIds = setOf(UserOne)
        givenAccounts(flowOf(listOf(account(UserTwo))))
        givenRustEnabled(UserTwo, enabled = false)
        givenOptedOut(UserTwo, optedOut = false)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { preferencesRepository.clearUserOptedOut(UserOne) }
    }

    @Test
    fun `does not start the sweep when there is no ready account`() = runTest {
        // Given
        givenAccounts(flowOf(emptyList()))

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { startContentIndexingSweep() }
    }

    @Test
    fun `resumes the sweep when the app returns to the foreground`() = runTest {
        // Given
        givenAccounts(flowOf(emptyList()))
        val appInBackground = MutableStateFlow(true)
        every { appInBackgroundState.observe() } returns appInBackground

        // When
        handler().start()
        appInBackground.value = false
        advanceUntilIdle() // let the foreground-resume debounce elapse

        // Then
        coVerify(exactly = 1) { resumeContentIndexingSweep() }
    }

    @Test
    fun `coalesces a burst of foreground flips into a single resume`() = runTest {
        // Given
        givenAccounts(flowOf(emptyList()))
        val appInBackground = MutableStateFlow(true)
        every { appInBackgroundState.observe() } returns appInBackground

        // When
        handler().start()
        appInBackground.value = false
        appInBackground.value = true
        appInBackground.value = false
        advanceUntilIdle() // debounce collapses the flips that settled within the window

        // Then
        coVerify(exactly = 1) { resumeContentIndexingSweep() }
    }

    @Test
    fun `does not resume the sweep while the app stays in the background`() = runTest {
        // Given
        givenAccounts(flowOf(emptyList()))
        every { appInBackgroundState.observe() } returns MutableStateFlow(true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { resumeContentIndexingSweep() }
    }

    private fun TestScope.handler() = ContentSearchAutoIndexingHandler(
        userSessionRepository = userSessionRepository,
        isContentSearchEnabled = isContentSearchEnabled,
        settingsRepository = settingsRepository,
        startContentIndexingSweep = startContentIndexingSweep,
        resumeContentIndexingSweep = resumeContentIndexingSweep,
        preferencesRepository = preferencesRepository,
        appInBackgroundState = appInBackgroundState,
        appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private fun givenAccounts(flow: Flow<List<Account>>) {
        every { userSessionRepository.observeAccounts() } returns flow
    }

    private fun givenRustEnabled(userId: UserId, enabled: Boolean) {
        coEvery { isContentSearchEnabled(userId) } returns enabled.right()
    }

    private fun givenOptedOut(userId: UserId, optedOut: Boolean) {
        coEvery { preferencesRepository.hasUserOptedOut(userId) } returns optedOut.right()
    }

    private fun account(userId: UserId, state: AccountState = AccountState.Ready) = Account(
        userId = userId,
        name = userId.id,
        state = state,
        primaryAddress = "${userId.id}@proton.me"
    )

    private companion object {
        val UserOne = UserId("user-1")
        val UserTwo = UserId("user-2")
    }
}
