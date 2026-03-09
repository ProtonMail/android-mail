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

package ch.protonmail.android.mailpinlock.data

import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionShouldAutoLockResult
import uniffi.mail_uniffi.SessionReason
import uniffi.mail_uniffi.UserSessionError
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RustAppLockDataSourceTest {

    private val sut = RustAppLockDataSource()

    @Test
    fun `when shouldAutoLock is TRUE then returns true right`() = runTest {
        val mailSession = mockk<MailSession> {
            coEvery { this@mockk.shouldAutoLock() } returns MailSessionShouldAutoLockResult.Ok(true)
        }
        val result = sut.shouldAutoLock(mailSession)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `when shouldAutoLock FALSE then returns false right`() = runTest {
        val mailSession = mockk<MailSession> {
            coEvery { this@mockk.shouldAutoLock() } returns MailSessionShouldAutoLockResult.Ok(false)
        }
        val result = sut.shouldAutoLock(mailSession)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `when shouldAutoLock and error then returns mapped error`() = runTest {
        val expectedError = DataError.Local.NotFound
        val mailSession = mockk<MailSession> {
            coEvery { this@mockk.shouldAutoLock() } returns
                MailSessionShouldAutoLockResult.Error(
                    UserSessionError.Reason(
                        SessionReason.UnknownLabel
                    )
                )
        }
        val result = sut.shouldAutoLock(mailSession)
        assertEquals(expectedError.left(), result)
    }
}
