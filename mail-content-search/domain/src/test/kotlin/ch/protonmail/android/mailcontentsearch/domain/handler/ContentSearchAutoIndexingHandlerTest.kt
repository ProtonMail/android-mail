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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import ch.protonmail.android.mailcontentsearch.domain.usecase.ApplyContentSearchMobileDataPreference
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
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
import kotlin.test.assertTrue

internal class ContentSearchAutoIndexingHandlerTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val isContentSearchEnabled = mockk<IsContentSearchEnabled> {
        coEvery { this@mockk.invoke(any()) } returns false.right()
    }
    private val applyMobileDataPreference = mockk<ApplyContentSearchMobileDataPreference>(relaxUnitFun = true)
    private val settingsRepository = mockk<ContentSearchSettingsRepository> {
        coEvery { setEnabled(any(), any()) } returns Unit.right()
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
    fun `enables a disabled account that has not opted out`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenRustEnabled(UserOne, enabled = false)
        givenOptedOut(UserOne, optedOut = false)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { settingsRepository.setEnabled(UserOne, enabled = true) }
    }

    @Test
    fun `does not re-enable an already-enabled account but clears a stale opt-out`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne))))
        givenRustEnabled(UserOne, enabled = true)
        givenOptedOut(UserOne, optedOut = true)

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
        coVerify(exactly = 1) { preferencesRepository.clearUserOptedOut(UserOne) }
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
    }

    @Test
    fun `ignores accounts that are not ready`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne, state = AccountState.NotReady))))

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { settingsRepository.setEnabled(any(), any()) }
    }

    @Test
    fun `enables a newly logged-in disabled account`() = runTest {
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
    fun `retries clearing the opt-out on the next emission when the clear fails`() = runTest {
        // Given
        persistedKnownUserIds = setOf(UserOne)
        givenAccounts(flowOf(listOf(account(UserTwo)), emptyList()))
        givenRustEnabled(UserTwo, enabled = false)
        givenOptedOut(UserTwo, optedOut = false)
        coEvery { preferencesRepository.clearUserOptedOut(UserOne) } returns PreferencesError.left()

        // When
        handler().start()

        // Then
        coVerify(exactly = 2) { preferencesRepository.clearUserOptedOut(UserOne) }
        assertTrue(UserOne in persistedKnownUserIds)
    }

    @Test
    fun `applies the mobile data preference to every ready account`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne), account(UserTwo, AccountState.NotReady))))
        givenRustEnabled(UserOne, enabled = true)
        givenOptedOut(UserOne, optedOut = false)

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { applyMobileDataPreference(UserOne) }
        coVerify(exactly = 0) { applyMobileDataPreference(UserTwo) }
    }

    private fun TestScope.handler() = ContentSearchAutoIndexingHandler(
        userSessionRepository = userSessionRepository,
        isContentSearchEnabled = isContentSearchEnabled,
        applyMobileDataPreference = applyMobileDataPreference,
        settingsRepository = settingsRepository,
        preferencesRepository = preferencesRepository,
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
