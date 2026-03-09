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

package ch.protonmail.android.mailsession.data.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.BackgroundExecutionCallback
import uniffi.mail_uniffi.BackgroundExecutionResult
import uniffi.mail_uniffi.BackgroundExecutionStatus
import uniffi.mail_uniffi.MailSessionStartBackgroundExecutionResult
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.UserSessionError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class StartBackgroundExecutionTest {

    private val mailSessionRepository = mockk<MailSessionRepository>()

    private val startBackgroundExecution = StartBackgroundExecution(mailSessionRepository)

    @Test
    fun `should trigger background execution and return the expected status when called`() = runTest {
        // Given
        val mailSessionWrapper = mockk<MailSessionWrapper>()
        val callback = slot<BackgroundExecutionCallback>()
        val executionResult = mockk<MailSessionStartBackgroundExecutionResult.Ok>()
        val expectedStatus = BackgroundExecutionResult(
            BackgroundExecutionStatus.Executed,
            hasUnsentMessages = false,
            hasPendingActions = false
        )
        every { mailSessionRepository.getMailSession() } returns mailSessionWrapper
        every {
            mailSessionWrapper.startBackgroundTask(capture(callback))
        } returns executionResult
        every { executionResult.destroy() } just runs

        // When
        startBackgroundExecution().test {
            callback.captured.onExecutionCompleted(expectedStatus)
            assertEquals(expectedStatus, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            mailSessionWrapper.startBackgroundTask(callback.captured)
            executionResult.destroy()
        }
    }

    @Test
    fun `should handle background execution error when called`() = runTest {
        // Given
        val mailSessionWrapper = mockk<MailSessionWrapper>()
        val callback = slot<BackgroundExecutionCallback>()
        val executionResult = mockk<MailSessionStartBackgroundExecutionResult.Error> {
            every { this@mockk.v1 } returns UserSessionError.Other(ProtonError.Network)
        }
        every { mailSessionRepository.getMailSession() } returns mailSessionWrapper
        every {
            mailSessionWrapper.startBackgroundTask(capture(callback))
        } returns executionResult

        // When
        startBackgroundExecution().test {
            assertTrue { awaitError() is RuntimeException }
        }

        // Then
        coVerify(exactly = 1) {
            mailSessionWrapper.startBackgroundTask(callback.captured)
        }

        coVerify(exactly = 0) { executionResult.destroy() }
    }
}
