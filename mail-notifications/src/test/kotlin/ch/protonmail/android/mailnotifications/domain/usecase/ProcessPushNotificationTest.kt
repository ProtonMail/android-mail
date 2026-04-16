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

package ch.protonmail.android.mailnotifications.domain.usecase

import androidx.work.ListenableWorker
import androidx.work.workDataOf
import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailnotifications.data.model.DecryptionError
import ch.protonmail.android.mailnotifications.data.usecase.DecryptPushNotificationContent
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.network.domain.session.SessionId
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ProcessPushNotificationTest {

    private val decryptPushNotificationContent = mockk<DecryptPushNotificationContent>()
    private val processNewMessagePushNotification = mockk<ProcessNewMessagePushNotification>()
    private val processNewLoginPushNotification = mockk<ProcessNewLoginPushNotification>()
    private val processMessageReadPushNotification = mockk<ProcessMessageReadPushNotification>()

    private val userIdValue = UserIdSample.Primary
    private val sessionIdValue = SessionId("sessionId")
    private val encryptedPayload = "notification"

    private val processPushNotification = ProcessPushNotification(
        decryptPushNotificationContent = decryptPushNotificationContent,
        processNewMessagePushNotification = processNewMessagePushNotification,
        processNewLoginPushNotification = processNewLoginPushNotification,
        processMessageReadPushNotification = processMessageReadPushNotification
    )

    @Test
    fun `decrypt failure makes processing fail`() = runTest {
        // Given
        val error = DecryptionError.FailedToDecrypt
        coEvery {
            decryptPushNotificationContent(userIdValue, sessionIdValue, encryptedPayload)
        } returns error.left()

        // When
        val result = processPushNotification(
            userId = userIdValue,
            sessionId = sessionIdValue,
            encryptedPayload = encryptedPayload
        )

        // Then
        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(
                    ProcessPushNotification.KeyProcessPushNotificationDataError to error.message
                )
            ),
            result
        )
    }

    @Test
    fun `new message notification is delegated to new message processor`() = runTest {
        // Given
        val notification = mockk<LocalPushNotification.Message.NewMessage>()
        val expected = ListenableWorker.Result.success()

        coEvery {
            decryptPushNotificationContent(userIdValue, sessionIdValue, encryptedPayload)
        } returns Either.Right(notification)
        coEvery { processNewMessagePushNotification(notification) } returns expected

        // When
        val result = processPushNotification(
            userId = userIdValue,
            sessionId = sessionIdValue,
            encryptedPayload = encryptedPayload
        )

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { processNewMessagePushNotification(notification) }
    }

    @Test
    fun `message read notification is delegated to message read processor`() = runTest {
        // Given
        val notification = mockk<LocalPushNotification.Message.MessageRead>()
        val expected = ListenableWorker.Result.success()

        coEvery {
            decryptPushNotificationContent(userIdValue, sessionIdValue, encryptedPayload)
        } returns Either.Right(notification)
        coEvery { processMessageReadPushNotification(notification) } returns expected

        // When
        val result = processPushNotification(
            userId = userIdValue,
            sessionId = sessionIdValue,
            encryptedPayload = encryptedPayload
        )

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { processMessageReadPushNotification(notification) }
    }

    @Test
    fun `login notification is delegated to login processor`() = runTest {
        // Given
        val notification = mockk<LocalPushNotification.Login>()
        val expected = ListenableWorker.Result.success()

        coEvery {
            decryptPushNotificationContent(userIdValue, sessionIdValue, encryptedPayload)
        } returns Either.Right(notification)
        coEvery { processNewLoginPushNotification(notification) } returns expected

        // When
        val result = processPushNotification(
            userId = userIdValue,
            sessionId = sessionIdValue,
            encryptedPayload = encryptedPayload
        )

        // Then
        assertEquals(expected, result)
        coVerify(exactly = 1) { processNewLoginPushNotification(notification) }
    }

    @Test
    fun `unsupported action makes processing fail`() = runTest {
        // Given
        val notification = mockk<LocalPushNotification.Message.UnsupportedMessageAction> {
            every { actionType?.action } returns "unsupported-action"
        }

        coEvery {
            decryptPushNotificationContent(userIdValue, sessionIdValue, encryptedPayload)
        } returns Either.Right(notification)

        // When
        val result = processPushNotification(
            userId = userIdValue,
            sessionId = sessionIdValue,
            encryptedPayload = encryptedPayload
        )

        // Then
        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(
                    ProcessPushNotification.KeyProcessPushNotificationDataError to
                        "Unsupported push action - unsupported-action"
                )
            ),
            result
        )
    }
}
