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

package ch.protonmail.android.mailnotifications.data.usecase

import ch.protonmail.android.mailnotifications.data.model.QuickActionPayloadData
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailsession.data.mapper.toLocalUserId
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ActionError
import uniffi.mail_uniffi.ActionErrorReason
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetSessionsResult
import uniffi.mail_uniffi.PushNotificationQuickAction
import uniffi.mail_uniffi.RemoteId
import uniffi.mail_uniffi.SessionReason
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.UserSessionError
import uniffi.mail_uniffi.VoidActionResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class ExecutePushNotificationActionTest {

    private val storedSession = mockk<StoredSession> {
        coEvery { this@mockk.userId() } returns userId.toLocalUserId()
    }
    private val mailSession = mockk<MailSession> {
        coEvery { this@mockk.getSessions() } returns
            MailSessionGetSessionsResult.Ok(listOf(storedSession))
    }

    private val mailSessionRepo = mockk<MailSessionRepository> {
        coEvery { this@mockk.getMailSession().getRustMailSession() } returns mailSession
    }

    private lateinit var executePushNotificationAction: ExecutePushNotificationAction

    @BeforeTest
    fun setup() {
        executePushNotificationAction = ExecutePushNotificationAction(mailSessionRepo)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should proxy the trigger to the correct action (mark read)`() = runTest {
        // Given
        val payload = QuickActionPayloadData(userId, remoteId, LocalNotificationAction.MarkAsRead)
        val expectedRustAction = PushNotificationQuickAction.MarkAsRead(RemoteId(remoteId))

        coEvery {
            mailSession.executeNotificationQuickAction(storedSession, any<PushNotificationQuickAction>(), null)
        } returns VoidActionResult.Ok

        // When
        val result = executePushNotificationAction(payload)

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 1) { mailSession.executeNotificationQuickAction(storedSession, expectedRustAction, null) }
        coVerify { mailSession.getSessions() }
        confirmVerified(mailSession)
    }

    @Test
    fun `should proxy the trigger to the correct action (archive)`() = runTest {
        // Given
        val payload = QuickActionPayloadData(userId, remoteId, LocalNotificationAction.MoveTo.Archive)
        val expectedRustAction = PushNotificationQuickAction.MoveToArchive(RemoteId(remoteId))

        coEvery {
            mailSession.executeNotificationQuickAction(storedSession, any<PushNotificationQuickAction>(), null)
        } returns VoidActionResult.Ok

        // When
        val result = executePushNotificationAction(payload)

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 1) { mailSession.executeNotificationQuickAction(storedSession, expectedRustAction, null) }
        coVerify { mailSession.getSessions() }
        confirmVerified(mailSession)
    }

    @Test
    fun `should proxy the trigger to the correct action (trash)`() = runTest {
        // Given
        val payload = QuickActionPayloadData(userId, remoteId, LocalNotificationAction.MoveTo.Trash)
        val expectedRustAction = PushNotificationQuickAction.MoveToTrash(RemoteId(remoteId))

        coEvery {
            mailSession.executeNotificationQuickAction(storedSession, any<PushNotificationQuickAction>(), null)
        } returns VoidActionResult.Ok

        // When
        val result = executePushNotificationAction(payload)

        // Then
        assertTrue(result.isRight())
        coVerify(exactly = 1) { mailSession.executeNotificationQuickAction(storedSession, expectedRustAction, null) }
        coVerify { mailSession.getSessions() }
        confirmVerified(mailSession)
    }

    @Test
    fun `should proxy the trigger to the correct action (error)`() = runTest {
        // Given
        val payload = QuickActionPayloadData(userId, remoteId, LocalNotificationAction.MoveTo.Trash)
        val expectedRustAction = PushNotificationQuickAction.MoveToTrash(RemoteId(remoteId))

        coEvery {
            mailSession.executeNotificationQuickAction(storedSession, any<PushNotificationQuickAction>(), null)
        } returns VoidActionResult.Error(ActionError.Reason(ActionErrorReason.UNKNOWN_MESSAGE))

        // When
        val result = executePushNotificationAction(payload)

        // Then
        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull() is QuickActionPushError.Error)
        coVerify(exactly = 1) { mailSession.executeNotificationQuickAction(storedSession, expectedRustAction, null) }
        coVerify { mailSession.getSessions() }
        confirmVerified(mailSession)
    }

    @Test
    fun `should proxy the trigger to the correct action (no session)`() = runTest {
        // Given
        val payload = QuickActionPayloadData(userId, remoteId, LocalNotificationAction.MoveTo.Trash)

        coEvery { mailSession.getSessions() } returns MailSessionGetSessionsResult.Error(
            UserSessionError.Reason(SessionReason.UnknownLabel)
        )

        // When
        val result = executePushNotificationAction(payload)

        // Then
        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull() is QuickActionPushError.NoMailSession)
        coVerify { mailSession.getSessions() }
        confirmVerified(mailSession)
    }

    private companion object {

        val userId = UserId("user-id")
        const val remoteId = "remoteId"
    }
}
