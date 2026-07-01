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
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetContentSearchEnabled
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class ContentSearchAutoIndexingHandlerTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val setContentSearchEnabled = mockk<SetContentSearchEnabled>()
    private val startContentIndexingSweep = mockk<StartContentIndexingSweep> {
        coEvery { this@mockk.invoke() } returns EnqueueIndexingResult.Scheduled
    }
    private val preferencesRepository = mockk<ContentSearchPreferencesRepository> {
        coEvery { markAutoEnableApplied(any()) } returns Unit.right()
        coEvery { clearAutoEnableApplied(any()) } returns Unit.right()
    }

    @Test
    fun `enables content search and starts the sweep on first sight of a ready account`() = runTest {
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenAlreadyApplied(UserOne, applied = false)
        givenEnableSucceeds(UserOne)

        handler().start()

        coVerify(exactly = 1) { setContentSearchEnabled(UserOne, enabled = true) }
        coVerify(exactly = 1) { preferencesRepository.markAutoEnableApplied(UserOne) }
        coVerify(exactly = 1) { startContentIndexingSweep() }
    }

    @Test
    fun `does not re-enable an account already auto-enabled but still resumes the sweep on launch`() = runTest {
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenAlreadyApplied(UserOne, applied = true)

        handler().start()

        coVerify(exactly = 0) { setContentSearchEnabled(any(), any()) }
        coVerify(exactly = 1) { startContentIndexingSweep() }
    }

    @Test
    fun `ignores accounts that are not ready`() = runTest {
        givenAccounts(flowOf(listOf(account(UserOne, state = AccountState.NotReady))))

        handler().start()

        coVerify(exactly = 0) { setContentSearchEnabled(any(), any()) }
        coVerify(exactly = 0) { startContentIndexingSweep() }
    }

    @Test
    fun `enables and restarts the sweep when a new account logs in`() = runTest {
        givenAccounts(flowOf(listOf(account(UserOne)), listOf(account(UserOne), account(UserTwo))))
        givenAlreadyApplied(UserOne, applied = true)
        givenAlreadyApplied(UserTwo, applied = false)
        givenEnableSucceeds(UserTwo)

        handler().start()

        coVerify(exactly = 1) { setContentSearchEnabled(UserTwo, enabled = true) }
        coVerify(exactly = 0) { setContentSearchEnabled(UserOne, any()) }
        // Once for the initial launch, once when the second account appears.
        coVerify(exactly = 2) { startContentIndexingSweep() }
    }

    @Test
    fun `restarts the sweep when an already-enabled account newly becomes ready`() = runTest {
        givenAccounts(
            flowOf(
                listOf(account(UserOne)),
                listOf(account(UserOne), account(UserTwo))
            )
        )
        givenAlreadyApplied(UserOne, applied = true)
        givenAlreadyApplied(UserTwo, applied = true)

        handler().start()

        coVerify(exactly = 0) { setContentSearchEnabled(any(), any()) }
        // Once for the initial launch, once when the second account becomes ready.
        coVerify(exactly = 2) { startContentIndexingSweep() }
    }

    @Test
    fun `clears the auto-enable marker when an account is signed out`() = runTest {
        givenAccounts(flowOf(listOf(account(UserOne)), emptyList()))
        givenAlreadyApplied(UserOne, applied = false)
        givenEnableSucceeds(UserOne)

        handler().start()

        coVerify(exactly = 1) { preferencesRepository.clearAutoEnableApplied(UserOne) }
    }

    @Test
    fun `does not start the sweep when there is no ready account`() = runTest {
        givenAccounts(flowOf(emptyList()))

        handler().start()

        coVerify(exactly = 0) { startContentIndexingSweep() }
    }

    private fun TestScope.handler() = ContentSearchAutoIndexingHandler(
        userSessionRepository = userSessionRepository,
        setContentSearchEnabled = setContentSearchEnabled,
        startContentIndexingSweep = startContentIndexingSweep,
        preferencesRepository = preferencesRepository,
        appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private fun givenAccounts(flow: Flow<List<Account>>) {
        every { userSessionRepository.observeAccounts() } returns flow
    }

    private fun givenAlreadyApplied(userId: UserId, applied: Boolean) {
        coEvery { preferencesRepository.hasAutoEnableBeenApplied(userId) } returns applied.right()
    }

    private fun givenEnableSucceeds(userId: UserId) {
        coEvery { setContentSearchEnabled(userId, enabled = true) } returns Unit.right()
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
