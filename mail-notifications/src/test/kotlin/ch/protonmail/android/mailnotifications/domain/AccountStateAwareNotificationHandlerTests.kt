/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailnotifications.domain

import ch.protonmail.android.mailnotifications.data.repository.NotificationTokenRepository
import ch.protonmail.android.mailnotifications.domain.handler.AccountStateAwareNotificationHandler
import ch.protonmail.android.mailnotifications.domain.usecase.DismissEmailNotificationsForUser
import ch.protonmail.android.testdata.AccountTestData
import io.mockk.called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import org.junit.After
import org.junit.Test

internal class AccountStateAwareNotificationHandlerTests {

    private val accountManager = mockk<AccountManager>()
    private val notificationTokenRepository: NotificationTokenRepository = mockk()
    private val dismissEmailNotificationsForUser: DismissEmailNotificationsForUser = mockk()
    private val scope = TestScope()
    private val notificationHandler = AccountStateAwareNotificationHandler(
        accountManager,
        notificationTokenRepository,
        dismissEmailNotificationsForUser,
        scope
    )

    @After
    fun resetMocks() {
        unmockkAll()
    }

    @Test
    fun `should call notification token registration when account state becomes ready`() = runTest {
        // given
        val expectedAccount = AccountTestData.readyAccount
        every { accountManager.onAccountStateChanged(true) } returns flowOf(expectedAccount)

        // when
        notificationHandler.handle()
        scope.advanceUntilIdle()

        // then
        coVerify(exactly = 1) { notificationTokenRepository.bindTokenToUser(expectedAccount.userId) }
        verify { dismissEmailNotificationsForUser wasNot called }
    }

    @Test
    fun `should call notifications dismissal when account state becomes disabled`() = runTest {
        // given
        val expectedAccount = AccountTestData.disabledAccount
        every { accountManager.onAccountStateChanged(true) } returns flowOf(expectedAccount)

        // when
        notificationHandler.handle()
        scope.advanceUntilIdle()

        // then
        verify(exactly = 1) { dismissEmailNotificationsForUser(expectedAccount.userId) }
        verify { notificationTokenRepository wasNot called }
    }

    @Test
    fun `should call notifications dismissal when account state becomes removed`() = runTest {
        // given
        val expectedAccount = AccountTestData.removedAccount
        every { accountManager.onAccountStateChanged(true) } returns flowOf(expectedAccount)

        // when
        notificationHandler.handle()
        scope.advanceUntilIdle()

        // then
        verify(exactly = 1) { dismissEmailNotificationsForUser(expectedAccount.userId) }
        verify { notificationTokenRepository wasNot called }
    }

    @Test
    fun `should do nothing when account state does not need any action`() {
        // given
        val expectedAccount = AccountTestData.notReadyAccount
        every { accountManager.onAccountStateChanged(true) } returns flowOf(expectedAccount)

        // when
        notificationHandler.handle()
        scope.advanceUntilIdle()

        // then
        verify { notificationTokenRepository wasNot called }
        verify { dismissEmailNotificationsForUser wasNot called }
    }
}
