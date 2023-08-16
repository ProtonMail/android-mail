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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class GetDecryptedDraftFieldsTest {

    private val messageRepository = mockk<MessageRepository>()
    private val getDecryptedMessageBody = mockk<GetDecryptedMessageBody>()

    private val getDecryptedDraftFields = GetDecryptedDraftFields(messageRepository, getDecryptedMessageBody)

    @Test
    fun `returns draft data when fetch and decrypt operations succeed`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val decryptedMessageBody = DecryptedMessageBodyTestData.buildDecryptedMessageBody()
        val expectedMessage = MessageWithBodySample.RemoteDraft
        expectedFetchMessage(userId, messageId) { expectedMessage }
        expectDecryptedMessageResult(userId, messageId) { decryptedMessageBody }

        // When
        val actual = getDecryptedDraftFields(userId, messageId)

        // Then
        val expected = expectedMessage.toDraftFields(decryptedMessageBody.value)
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `get message from local when fetch message fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val decryptedMessageBody = DecryptedMessageBodyTestData.buildDecryptedMessageBody()
        val expectedMessage = MessageWithBodySample.RemoteDraft
        expectedFetchMessageError(userId, messageId) { DataError.Remote.Http(NetworkError.ServerError) }
        expectedGetLocalMessage(userId, messageId) { expectedMessage }
        expectDecryptedMessageResult(userId, messageId) { decryptedMessageBody }

        // When
        val actual = getDecryptedDraftFields(userId, messageId)

        // Then
        val expected = expectedMessage.toDraftFields(decryptedMessageBody.value)
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns data error when both fetch message and get local message with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val fetchError = DataError.Remote.Http(NetworkError.NoNetwork)
        expectedFetchMessageError(userId, messageId) { fetchError }
        expectGetLocalMessageFailure(userId, messageId)

        // When
        val actual = getDecryptedDraftFields(userId, messageId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns decryption error when decrypt message with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        val expectedMessage = MessageWithBodySample.RemoteDraft
        val decryptError = GetDecryptedMessageBodyError.Decryption("Failed decrypting")
        expectedFetchMessage(userId, messageId) { expectedMessage }
        expectDecryptedMessageError(userId, messageId) { decryptError }

        // When
        val actual = getDecryptedDraftFields(userId, messageId)

        // Then
        assertEquals(DataError.Local.DecryptionError.left(), actual)
    }


    private fun expectDecryptedMessageError(
        userId: UserId,
        messageId: MessageId,
        error: () -> GetDecryptedMessageBodyError
    ) = error().also {
        coEvery { getDecryptedMessageBody(userId, messageId) } returns it.left()
    }

    private fun expectGetLocalMessageFailure(userId: UserId, messageId: MessageId) {
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns null
    }

    private fun expectedFetchMessageError(
        userId: UserId,
        messageId: MessageId,
        error: () -> DataError.Remote
    ) = error().also {
        coEvery { messageRepository.fetchAndStoreMessageWithBody(userId, messageId) } returns it.left()
    }

    private fun expectDecryptedMessageResult(
        userId: UserId,
        messageId: MessageId,
        result: () -> DecryptedMessageBody
    ) = result().also {
        coEvery { getDecryptedMessageBody(userId, messageId) } returns it.right()
    }

    private fun expectedGetLocalMessage(
        userId: UserId,
        messageId: MessageId,
        result: () -> MessageWithBody
    ) = result().also {
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns it
    }

    private fun expectedFetchMessage(
        userId: UserId,
        messageId: MessageId,
        result: () -> MessageWithBody
    ) = result().also {
        coEvery { messageRepository.fetchAndStoreMessageWithBody(userId, messageId) } returns it.right()
    }

    private fun MessageWithBody.toDraftFields(decryptedBody: String) = with(message) {
        DraftFields(
            SenderEmail(this.sender.address),
            Subject(this.subject),
            DraftBody(decryptedBody),
            RecipientsTo(this.toList),
            RecipientsCc(this.ccList),
            RecipientsBcc(this.bccList)
        )
    }
}
