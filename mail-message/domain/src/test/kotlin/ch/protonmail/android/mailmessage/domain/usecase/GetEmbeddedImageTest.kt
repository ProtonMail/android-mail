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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetEmbeddedImageTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("embeddedImageId")
    private val contentId = "embeddedImageContentId"

    private val attachmentRepository = mockk<AttachmentRepository>()
    private val messageRepository = mockk<MessageRepository>()

    private val getEmbeddedImage = GetEmbeddedImage(attachmentRepository, messageRepository)

    @Test
    fun `returns input stream and mime type of embedded image when getting it is successful and mime type is image`() =
        runTest {
            // Given
            val expectedByteArray = "I'm a bytearray".toByteArray()
            val expected = GetEmbeddedImageResult(expectedByteArray, "image/png").right()
            coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
                message = MessageTestData.message,
                messageBody = MessageBodyTestData.messageBodyWithEmbeddedImage
            )
            coEvery {
                attachmentRepository.getEmbeddedImage(userId, messageId, attachmentId)
            } returns expectedByteArray.right()

            // When
            val actual = getEmbeddedImage(userId, messageId, contentId)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `returns input stream and mime type of octet-stream embedded image when getting is successful `() = runTest {
        // Given
        val expectedByteArray = "I'm a bytearray".toByteArray()
        val expected = GetEmbeddedImageResult(expectedByteArray, "application/octet-stream").right()
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBodyWithEmbeddedOctetStream
        )
        coEvery {
            attachmentRepository.getEmbeddedImage(userId, messageId, attachmentId)
        } returns expectedByteArray.right()

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return data not cached error when embedded image mime type is not image or octet-stream`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBodyWithInvalidEmbeddedAttachment
        )

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns data not cached error when message with body is not found`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns null

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns data not cached error when embedded image attachment is not found`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBody
        )

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns data not cached error when no attachment with given content id is found`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBodyWithAttachment
        )

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `returns remote error when getting embedded image fails with a remote error`() = runTest {
        // Given
        val expected = DataError.Remote.Http(NetworkError.NoNetwork).left()
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBodyWithEmbeddedOctetStream
        )
        coEvery {
            attachmentRepository.getEmbeddedImage(userId, messageId, attachmentId)
        } returns expected

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Given
        assertEquals(expected, actual)
    }

    @Test
    fun `returns decrypt error when getting embedded image fails to decrypt`() = runTest {
        // Given
        val expected = DataError.Local.DecryptionError.left()
        coEvery {
            messageRepository.getLocalMessageWithBody(
                userId,
                messageId
            )
        } returns MessageWithBody(
            message = MessageTestData.message,
            messageBody = MessageBodyTestData.messageBodyWithEmbeddedOctetStream
        )
        coEvery {
            attachmentRepository.getEmbeddedImage(userId, messageId, attachmentId)
        } returns expected

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Given
        assertEquals(expected, actual)
    }

    @Test
    fun `returns no data cached when getting embedded images from a header list element`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns MessageWithBody(
            message = MessageTestData.message.copy(),
            messageBody = MessageBodyTestData.messageBodyWithContentIdList
        )

        // When
        val actual = getEmbeddedImage(userId, messageId, contentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }
}
