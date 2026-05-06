/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.repository

import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.MailBackgroundExecScope
import uniffi.mail_uniffi.MailSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MailSessionRepositoryExtensionsTest {

    private val scope = mockk<MailBackgroundExecScope>()
    private val rustMailSession = mockk<MailSession> {
        every { newBackgroundExecutionScope() } returns scope
    }
    private val wrapper = mockk<MailSessionWrapper> {
        every { getRustMailSession() } returns rustMailSession
        every { newBackgroundExecutionScope() } returns scope
    }
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { getMailSession() } returns wrapper
    }

    @Test
    fun `calls finishes after block completes successfully`() = runTest {
        // Given
        justRun { scope.finsihed() }

        // When
        val result = mailSessionRepository.runInRustBackground { "ok" }

        // Then
        assertEquals("ok", result)
        verify(exactly = 1) { scope.finsihed() }
    }

    @Test
    fun `calls finishes when block throws`() = runTest {
        // Given
        justRun { scope.finsihed() }
        val boom = IllegalStateException("boom")

        // When
        val thrown = assertFailsWith<IllegalStateException> {
            mailSessionRepository.runInRustBackground { throw boom }
        }

        // Then
        assertEquals(boom, thrown)
        verify(exactly = 1) { scope.finsihed() }
    }

    @Test
    fun `calls finishes exactly once even when block does early return`() = runTest {
        // Given
        justRun { scope.finsihed() }

        // When
        val result = earlyReturning()

        // Then
        assertEquals(Unit, result)
        verify(exactly = 1) { scope.finsihed() }
    }

    private suspend fun earlyReturning() = mailSessionRepository.runInRustBackground {
        if (System.currentTimeMillis() > 0) return@runInRustBackground
        error("unreachable")
    }
}
