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

import ch.protonmail.android.mailcontentsearch.domain.usecase.ApplyContentSearchMobileDataPreference
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
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

internal class ContentSearchAccountReadyHandlerTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val applyMobileDataPreference = mockk<ApplyContentSearchMobileDataPreference>(relaxUnitFun = true)

    @Test
    fun `applies the mobile data preference to every ready account`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne), account(UserTwo))))

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { applyMobileDataPreference(UserOne) }
        coVerify(exactly = 1) { applyMobileDataPreference(UserTwo) }
    }

    @Test
    fun `ignores accounts that are not ready`() = runTest {
        // Given - an account that is still unlocking has no user session to write to yet.
        givenAccounts(flowOf(listOf(account(UserOne, state = AccountState.NotReady))))

        // When
        handler().start()

        // Then
        coVerify(exactly = 0) { applyMobileDataPreference(any()) }
    }

    @Test
    fun `applies the preference to an account that becomes ready later`() = runTest {
        // Given
        givenAccounts(
            flowOf(
                listOf(account(UserOne, state = AccountState.NotReady)),
                listOf(account(UserOne))
            )
        )

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { applyMobileDataPreference(UserOne) }
    }

    @Test
    fun `applies the preference to a newly logged-in account`() = runTest {
        // Given
        givenAccounts(flowOf(listOf(account(UserOne)), listOf(account(UserOne), account(UserTwo))))

        // When
        handler().start()

        // Then
        coVerify(exactly = 1) { applyMobileDataPreference(UserTwo) }
    }

    private fun TestScope.handler() = ContentSearchAccountReadyHandler(
        userSessionRepository = userSessionRepository,
        applyMobileDataPreference = applyMobileDataPreference,
        appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private fun givenAccounts(flow: Flow<List<Account>>) {
        every { userSessionRepository.observeAccounts() } returns flow
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
