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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class FindFirstEligibleAccountToIndexTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val isContentSearchEnabled = mockk<IsContentSearchEnabled>()
    private val getContentSearchIndexingStatus = mockk<GetContentSearchIndexingStatus>()

    private val findFirstEligibleAccountToIndex = FindFirstEligibleAccountToIndex(
        userSessionRepository,
        isContentSearchEnabled,
        getContentSearchIndexingStatus
    )

    @Test
    fun `returns null when there are no accounts`() = runTest {
        // Given
        givenAccounts(emptyList())
        givenPrimary(null)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertNull(result)
    }

    @Test
    fun `returns the first enabled account whose indexing is not completed`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserOne, enabled = true)
        givenStatus(UserOne, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserOne, result)
    }

    @Test
    fun `skips accounts that are not ready`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne, state = AccountState.Disabled), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `skips accounts that have content search disabled`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserOne, enabled = false)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `skips accounts whose enabled check fails`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        coEvery { isContentSearchEnabled(UserOne) } returns DataError.Local.Unknown.left()
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `skips accounts whose indexing already completed`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserOne, enabled = true)
        givenStatus(UserOne, ContentIndexingState.Completed)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `returns null when every account is completed or ineligible`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserOne, enabled = true)
        givenStatus(UserOne, ContentIndexingState.Completed)
        givenEnabled(UserTwo, enabled = false)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertNull(result)
    }

    @Test
    fun `skips accounts present in the skip set`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(null)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex(skip = setOf(UserOne))

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `returns null when the only eligible account is skipped`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne)))
        givenPrimary(null)

        // When
        val result = findFirstEligibleAccountToIndex(skip = setOf(UserOne))

        // Then
        assertNull(result)
    }

    @Test
    fun `prioritises the primary account when it is eligible`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(UserTwo)
        givenEnabled(UserOne, enabled = true)
        givenStatus(UserOne, ContentIndexingState.Idle)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserTwo, result)
    }

    @Test
    fun `falls back to other accounts when the primary is not eligible`() = runTest {
        // Given
        givenAccounts(listOf(account(UserOne), account(UserTwo)))
        givenPrimary(UserTwo)
        givenEnabled(UserTwo, enabled = true)
        givenStatus(UserTwo, ContentIndexingState.Completed)
        givenEnabled(UserOne, enabled = true)
        givenStatus(UserOne, ContentIndexingState.Idle)

        // When
        val result = findFirstEligibleAccountToIndex()

        // Then
        assertEquals(UserOne, result)
    }

    private fun givenAccounts(accounts: List<Account>) {
        every { userSessionRepository.observeAccounts() } returns flowOf(accounts)
    }

    private fun givenPrimary(userId: UserId?) {
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
    }

    private fun givenEnabled(userId: UserId, enabled: Boolean) {
        coEvery { isContentSearchEnabled(userId) } returns enabled.right()
    }

    private fun givenStatus(userId: UserId, state: ContentIndexingState) {
        coEvery { getContentSearchIndexingStatus(userId) } returns state
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
