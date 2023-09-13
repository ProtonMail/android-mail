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
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
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
import kotlin.test.assertTrue

class GetLocalMessageDecryptedTest {

    private val messageRepository = mockk<MessageRepository>()
    private val getDecryptedMessageBody = mockk<GetDecryptedMessageBody>()

    private val getDraftFieldsFromParent = GetLocalMessageDecrypted(
        messageRepository,
        getDecryptedMessageBody
    )

    @Test
    fun `returns message with decrypted body when get local message and decrypt operations succeed`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        expectedGetLocalMessage(userId, messageId) { MessageWithBodySample.Invoice }
        expectDecryptedMessageResult(userId, messageId) {
            DecryptedMessageBodyTestData.buildDecryptedMessageBody()
        }

        // When
        val actual = getDraftFieldsFromParent(userId, messageId)

        // Then
        assertTrue(actual.isRight())
    }

    @Test
    fun `returns no cached data error when get local message with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        expectGetLocalMessageFailure(userId, messageId)

        // When
        val actual = getDraftFieldsFromParent(userId, messageId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns decryption error when decrypt message with body fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RemoteDraft
        expectedGetLocalMessage(userId, messageId) { MessageWithBodySample.RemoteDraft }
        expectDecryptedMessageError(userId, messageId) {
            GetDecryptedMessageBodyError.Decryption(messageId, "Failed decrypting")
        }

        // When
        val actual = getDraftFieldsFromParent(userId, messageId)

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
}
