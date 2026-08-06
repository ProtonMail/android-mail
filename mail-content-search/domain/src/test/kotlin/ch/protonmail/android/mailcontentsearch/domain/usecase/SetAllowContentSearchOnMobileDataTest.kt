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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class SetAllowContentSearchOnMobileDataTest {

    private val repository = mockk<ContentSearchPreferencesRepository> {
        coEvery { setAllowMobileData(any()) } returns Unit.right()
    }
    private val contentSearchRepository = mockk<ContentSearchRepository> {
        coEvery { setMeteredConnectionAllowed(any(), any()) } returns Unit.right()
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        every { observeAccounts() } returns flowOf(emptyList())
    }

    private val setAllowContentSearchOnMobileData = SetAllowContentSearchOnMobileData(
        repository,
        contentSearchRepository,
        userSessionRepository
    )

    @Test
    fun `delegates persisting the allow mobile data value to the repository`() = runTest {
        // When
        setAllowContentSearchOnMobileData(true)

        // Then
        coVerify { repository.setAllowMobileData(true) }
    }

    @Test
    fun `delegates disabling the allow mobile data value to the repository`() = runTest {
        // When
        setAllowContentSearchOnMobileData(false)

        // Then
        coVerify { repository.setAllowMobileData(false) }
    }

    @Test
    fun `fans the value out to every ready account`() = runTest {
        // Given
        givenAccounts(account(UserOne), account(UserTwo))

        // When
        setAllowContentSearchOnMobileData(false)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserOne, false) }
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserTwo, false) }
    }

    @Test
    fun `skips accounts that are not ready`() = runTest {
        // Given
        givenAccounts(account(UserOne), account(UserTwo, AccountState.NotReady))

        // When
        setAllowContentSearchOnMobileData(true)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserOne, true) }
        coVerify(exactly = 0) { contentSearchRepository.setMeteredConnectionAllowed(UserTwo, any()) }
    }

    @Test
    fun `keeps fanning out when one account fails`() = runTest {
        // Given
        givenAccounts(account(UserOne), account(UserTwo))
        coEvery {
            contentSearchRepository.setMeteredConnectionAllowed(UserOne, false)
        } returns DataError.Local.Unknown.left()

        // When
        setAllowContentSearchOnMobileData(false)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserTwo, false) }
    }

    private fun givenAccounts(vararg accounts: Account) {
        every { userSessionRepository.observeAccounts() } returns flowOf(accounts.toList())
    }

    private fun account(userId: UserId, state: AccountState = AccountState.Ready) = Account(
        userId = userId,
        name = userId.id,
        state = state,
        primaryAddress = "${userId.id}@proton.me"
    )

    private companion object {

        val UserOne = UserId("user-one")
        val UserTwo = UserId("user-two")
    }
}
