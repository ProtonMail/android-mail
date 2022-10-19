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

package ch.protonmail.android.mailcommon.domain.usecase

import java.io.IOException
import app.cash.turbine.test
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.assertIs
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ObservePrimaryUserTest {

    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val userFlow = MutableSharedFlow<User?>()
    private val userManager = mockk<UserManager> {
        every { this@mockk.observeUser(UserIdTestData.userId) } returns userFlow
    }

    private lateinit var observeUser: ObservePrimaryUser

    @Before
    fun setUp() {
        observeUser = ObservePrimaryUser(
            accountManager,
            userManager
        )
    }

    @Test
    fun `returns user when user manager returns valid user`() = runTest {
        observeUser.invoke().test {
            // Given
            primaryUserIdIs(UserIdTestData.userId)

            // When
            userManagerSuccessfullyReturns(UserTestData.Primary)

            // Then
            val actual = awaitItem()
            assertEquals(UserTestData.Primary, actual)
        }
    }

    @Test
    fun `returns exception when user manager returns an error`() = runTest {
        observeUser.invoke().test {
            // Given
            every { userManager.observeUser(UserIdTestData.userId) } throws IOException("Test")

            // When
            primaryUserIdIs(UserIdTestData.userId)

            // Then
            assertIs<IOException>(awaitError())
        }
    }

    @Test
    fun `returns null when there is no valid userId`() = runTest {
        observeUser.invoke().test {
            // Given
            primaryUserIdIs(null)

            // Then
            assertNull(awaitItem())
        }
    }

    private suspend fun primaryUserIdIs(userId: UserId?) {
        userIdFlow.emit(userId)
    }

    private suspend fun userManagerSuccessfullyReturns(user: User) {
        userFlow.emit(user)
    }
}
