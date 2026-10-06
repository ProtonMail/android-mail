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


package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPromptRepository
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ResetContentSearchPromptsOnSignOutTest {

    private val userId = UserId("user-1")
    private val otherUserId = UserId("user-2")

    private val accounts = MutableStateFlow<List<Account>>(emptyList())
    private val userSessionRepository = mockk<UserSessionRepository> {
        every { observeAccounts() } returns accounts
    }
    private val promptRepository = mockk<ContentSearchPromptRepository> {
        coEvery { reset(any(), any()) } returns Unit.right()
        coEvery { retainOnly(any()) } returns Unit.right()
    }

    @Test
    fun `skips the empty list the accounts start out with`() = runTest {
        // When
        startObserving()

        // Then
        coVerify(exactly = 0) { promptRepository.retainOnly(any()) }
    }

    @Test
    fun `keeps only the answers of signed in accounts on the first list`() = runTest {
        // Given
        startObserving()

        // When
        accounts.value = listOf(account(userId, AccountState.Ready), account(otherUserId, AccountState.Disabled))

        // Then
        coVerify(exactly = 1) { promptRepository.retainOnly(setOf(userId)) }
        coVerify(exactly = 0) { promptRepository.reset(any(), any()) }
    }

    @Test
    fun `drops the answers of accounts still signing in on the first list`() = runTest {
        // Given
        startObserving()

        // When
        accounts.value = listOf(account(userId, AccountState.Ready), account(otherUserId, AccountState.NotReady))

        // Then
        coVerify(exactly = 1) { promptRepository.retainOnly(setOf(userId)) }
    }

    @Test
    fun `resets every prompt when an account signs out`() = runTest {
        // Given
        startObservingWith(account(userId, AccountState.Ready))

        // When
        accounts.value = listOf(account(userId, AccountState.Disabled))

        // Then
        ContentSearchPrompt.entries.forEach { prompt ->
            coVerify(exactly = 1) { promptRepository.reset(userId, prompt) }
        }
    }

    @Test
    fun `does not reset a signed out account again when another account changes`() = runTest {
        // Given
        startObservingWith(account(userId, AccountState.Disabled), account(otherUserId, AccountState.Ready))

        // When
        accounts.value = listOf(
            account(userId, AccountState.Disabled),
            account(otherUserId, AccountState.Ready, name = "renamed")
        )

        // Then
        coVerify(exactly = 0) { promptRepository.reset(any(), any()) }
    }

    @Test
    fun `resets every prompt when an account is removed`() = runTest {
        // Given
        startObservingWith(account(userId, AccountState.Ready), account(otherUserId, AccountState.Ready))

        // When
        accounts.value = listOf(account(otherUserId, AccountState.Ready))

        // Then
        ContentSearchPrompt.entries.forEach { prompt ->
            coVerify(exactly = 1) { promptRepository.reset(userId, prompt) }
        }
        coVerify(exactly = 0) { promptRepository.reset(otherUserId, any()) }
    }

    @Test
    fun `resets every prompt when the last account is removed`() = runTest {
        // Given
        startObservingWith(account(userId, AccountState.Ready))

        // When
        accounts.value = emptyList()

        // Then
        ContentSearchPrompt.entries.forEach { prompt ->
            coVerify(exactly = 1) { promptRepository.reset(userId, prompt) }
        }
    }

    @Test
    fun `keeps the prompts of an account that signs in`() = runTest {
        // Given
        startObservingWith(account(otherUserId, AccountState.Ready))

        // When
        accounts.value = listOf(account(otherUserId, AccountState.Ready), account(userId, AccountState.NotReady))
        accounts.value = listOf(account(otherUserId, AccountState.Ready), account(userId, AccountState.Ready))

        // Then
        coVerify(exactly = 0) { promptRepository.reset(any(), any()) }
    }

    @Test
    fun `still resets the other prompts when one could not be reset`() = runTest {
        // Given
        coEvery { promptRepository.reset(userId, ContentSearchPrompt.SetUpOffer) } returns PreferencesError.left()
        startObservingWith(account(userId, AccountState.Ready))

        // When
        accounts.value = listOf(account(userId, AccountState.Disabled))

        // Then
        coVerify { promptRepository.reset(userId, ContentSearchPrompt.IndexingComplete) }
    }

    /** Starts observing and lets the first list through, so the test only sees what comes after. */
    private fun TestScope.startObservingWith(vararg initial: Account) {
        startObserving()
        accounts.value = initial.toList()
        clearMocks(promptRepository, answers = false)
    }

    private fun TestScope.startObserving() {
        // Unconfined, so each account list is handled as soon as it is emitted.
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        ResetContentSearchPromptsOnSignOut(userSessionRepository, promptRepository, scope).invoke()
    }

    private fun account(
        userId: UserId,
        state: AccountState,
        name: String = "name"
    ) = Account(
        userId = userId,
        name = name,
        state = state,
        primaryAddress = "address@proton.me"
    )
}
