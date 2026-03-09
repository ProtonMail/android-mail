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

package ch.protonmail.android.mailsession.data.user

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.LocalUserTestData
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.AsyncLiveQueryCallback
import uniffi.mail_uniffi.MailUserSessionWatchUserResult
import uniffi.mail_uniffi.MailUserSessionWatchUserStreamResult
import uniffi.mail_uniffi.WatchHandle
import uniffi.mail_uniffi.WatchUserStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RustUserDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: RustUserDataSource

    @BeforeTest
    fun setup() {
        dataSource = RustUserDataSourceImpl()
    }

    @Test
    fun `observe user returns flow of user successfully`() = runTest {
        // Given
        val user = LocalUserTestData.build()

        val wrapper = mockk<MailUserSessionWrapper>()
        val stream = mockk<WatchUserStream>()

        coEvery { wrapper.getUser() } returns user.right()
        coEvery { wrapper.watchUserStream() } returns MailUserSessionWatchUserStreamResult.Ok(stream)

        // When
        dataSource.observeUser(wrapper).test {
            // Then
            val emission = awaitItem()
            assertTrue(emission.isRight())
            assertEquals(user, emission.getOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observe user returns flow of error when datasource errors`() = runTest {
        // Given
        val expected = DataError.Local.NoUserSession

        val wrapper = mockk<MailUserSessionWrapper>()
        val handle = mockk<WatchHandle>(relaxed = true)
        val callbackSlot = slot<AsyncLiveQueryCallback>()

        coEvery { wrapper.getUser() } returns expected.left()
        coEvery { wrapper.watchUser(capture(callbackSlot)) } returns MailUserSessionWatchUserResult.Ok(handle)

        // When
        dataSource.observeUser(wrapper).test {
            // Then
            val emission = awaitItem()
            assertTrue(emission.isLeft())
            assertEquals(expected, emission.swap().getOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
