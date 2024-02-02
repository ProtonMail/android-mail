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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.Called
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportPhishingMessageTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice

    private val repository = mockk<MessageRepository>()
    private val moveMessage = mockk<MoveMessage>()
    private val decryptedMessageBody = mockk<GetDecryptedMessageBody>()

    private val reportPhishingMessage by lazy {
        ReportPhishingMessage(repository, moveMessage, decryptedMessageBody)
    }

    @Test
    fun `return Unit when report phishing was successful`() = runTest {
        // Given
        val expectedDecryptedMessage = DecryptedMessageBodyTestData.PlainTextDecryptedBody
        coEvery { decryptedMessageBody(userId, messageId) } returns expectedDecryptedMessage.right()
        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()
        coEvery { repository.reportPhishing(userId, expectedDecryptedMessage) } returns Unit.right()

        // When
        val result = reportPhishingMessage(userId, messageId)

        // Then
        assertEquals(Unit.right(), result)
        coVerifyOrder {
            decryptedMessageBody(userId, messageId)
            repository.reportPhishing(userId, expectedDecryptedMessage)
            moveMessage(userId, messageId, SystemLabelId.Spam.labelId)
        }
    }

    @Test
    fun `return FailedToGetDecryptedMessage when failed to get decrypted message`() = runTest {
        // Given
        val expectedError = ReportPhishingMessage.ReportPhishingError.FailedToGetDecryptedMessage
        coEvery {
            decryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

        // When
        val result = reportPhishingMessage(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
        coVerify { decryptedMessageBody(userId, messageId) }
        coVerify { repository wasNot Called }
    }

    @Test
    fun `return FailedToReportPhishing when failed to report phishing`() = runTest {
        // Given
        val expectedDecryptedMessage = DecryptedMessageBodyTestData.PlainTextDecryptedBody
        val expectedError = ReportPhishingMessage.ReportPhishingError.FailedToReportPhishing
        coEvery { decryptedMessageBody(userId, messageId) } returns expectedDecryptedMessage.right()
        coEvery { repository.reportPhishing(userId, expectedDecryptedMessage) } returns DataError.Remote.Unknown.left()
        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns Unit.right()

        // When
        val result = reportPhishingMessage(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
        coVerifyOrder {
            decryptedMessageBody(userId, messageId)
            repository.reportPhishing(userId, expectedDecryptedMessage)
            moveMessage wasNot called
        }
    }

    @Test
    fun `return FailedToMoveToSpam when failed to move message to spam`() = runTest {
        // Given
        val expectedDecryptedMessage = DecryptedMessageBodyTestData.PlainTextDecryptedBody
        val expectedError = ReportPhishingMessage.ReportPhishingError.FailedToMoveToSpam
        coEvery { decryptedMessageBody(userId, messageId) } returns expectedDecryptedMessage.right()
        coEvery { repository.reportPhishing(userId, expectedDecryptedMessage) } returns Unit.right()
        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) } returns DataError.Local.Unknown.left()

        // When
        val result = reportPhishingMessage(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
        coVerifyOrder {
            decryptedMessageBody(userId, messageId)
            repository.reportPhishing(userId, expectedDecryptedMessage)
            moveMessage(userId, messageId, SystemLabelId.Spam.labelId)
        }
    }

}
