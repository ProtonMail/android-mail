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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailnotifications.NotificationTestData
import ch.protonmail.android.mailnotifications.data.model.DecryptionError
import ch.protonmail.android.mailnotifications.data.usecase.DecryptPushNotification
import ch.protonmail.android.mailnotifications.data.usecase.DecryptPushNotificationContent
import ch.protonmail.android.mailnotifications.data.wrapper.DecryptedPushNotificationWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import uniffi.mail_uniffi.EncryptedPushNotification
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionUserResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class DecryptPushNotificationContentTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val decryptPushNotification = mockk<DecryptPushNotification>()

    private lateinit var decryptPushNotificationContent: DecryptPushNotificationContent

    @BeforeTest
    fun setup() {
        decryptPushNotificationContent = DecryptPushNotificationContent(
            userSessionRepository,
            decryptPushNotification
        )
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return unknown user error when unable to determine user session`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(NotificationTestData.userId) } returns null
        val expectedError = DecryptionError.UnknownUser

        // When
        val result = decryptPushNotificationContent(
            userId = NotificationTestData.userId,
            sessionId = NotificationTestData.sessionId,
            encryptedContent = "something"
        )

        // Then
        assertEquals(result, expectedError.left())
    }

    @Test
    fun `should return user email error when unable to determine user email`() = runTest {
        // Given
        expectUserSessionError(NotificationTestData.userId)
        val expectedError = DecryptionError.FailedToDetermineUserEmail

        // When
        val result = decryptPushNotificationContent(
            userId = NotificationTestData.userId,
            sessionId = NotificationTestData.sessionId,
            encryptedContent = "something"
        )

        // Then
        assertEquals(result, expectedError.left())
    }

    @Test
    fun `should return an error when is unable to decrypt the content`() = runTest {
        // Given
        val expectedError = DecryptionError.FailedToDecrypt.left()

        expectValidUserSession(NotificationTestData.userId, NotificationTestData.email)
        coEvery { decryptPushNotification.invoke(any()) } returns expectedError

        // When
        val result = decryptPushNotificationContent(
            userId = NotificationTestData.userId,
            sessionId = NotificationTestData.sessionId,
            encryptedContent = "something"
        )

        assertEquals(result, expectedError)
    }

    @Test
    fun `should return a wrapped decrypted notification when is able to decrypt the content`() = runTest {
        // Given
        expectValidUserSession(NotificationTestData.userId, NotificationTestData.email)

        val encryptedPushNotification = EncryptedPushNotification(
            sessionId = NotificationTestData.sessionId.id,
            encryptedMessage = "encryptedMessage"
        )

        val expectedWrapper = DecryptedPushNotificationWrapper.Email(
            NotificationTestData.decryptedEmailPushNotification
        ).right()

        coEvery {
            decryptPushNotification.invoke(encryptedPushNotification)
        } returns expectedWrapper

        // When
        val result = decryptPushNotificationContent(
            userId = NotificationTestData.userId,
            sessionId = NotificationTestData.sessionId,
            encryptedContent = encryptedPushNotification.encryptedMessage
        )

        assertEquals(result, NotificationTestData.defaultNewMessageNotification.right())
    }

    private fun expectUserSessionError(userId: UserId) {
        val mailUserSession = mockk<MailUserSession> {
            coEvery { this@mockk.user() } returns mockk<MailUserSessionUserResult.Error>()
        }

        val sessionWrapperMock = mockk<MailUserSessionWrapper> {
            every { this@mockk.getRustUserSession() } returns mailUserSession
        }

        coEvery { userSessionRepository.getUserSession(userId) } returns sessionWrapperMock
    }

    private fun expectValidUserSession(userId: UserId, email: String): MailUserSession {
        val userMock = mockk<MailUserSessionUserResult.Ok> {
            every { this@mockk.v1.email } returns email
        }

        val mailUserSession = mockk<MailUserSession> {
            coEvery { this@mockk.user() } returns userMock
        }

        val sessionWrapperMock = mockk<MailUserSessionWrapper> {
            every { this@mockk.getRustUserSession() } returns mailUserSession
        }

        coEvery { userSessionRepository.getUserSession(userId) } returns sessionWrapperMock

        return mailUserSession
    }
}
