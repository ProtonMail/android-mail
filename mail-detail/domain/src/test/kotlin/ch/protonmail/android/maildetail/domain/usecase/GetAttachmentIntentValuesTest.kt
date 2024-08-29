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

import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAttachmentIntentValuesTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("invoice")

    private val extension = "txt"
    private val uri = mockk<Uri>()

    private val messageAttachmentMetadata by lazy {
        MessageAttachmentMetadata(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            uri = Uri.parse("/test/tmp.$extension"),
            status = AttachmentWorkerStatus.Success
        )
    }

    private val messageWithBody = MessageWithBody(
        message = MessageTestData.message,
        messageBody = MessageBodyTestData.messageBodyWithAttachment
    )
    private val pgpMimeMessageWithBody = MessageWithBodySample.PgpMimeMessageWithAttachment

    private val attachmentRepository = mockk<AttachmentRepository>()
    private val messageRepository = mockk<MessageRepository>()

    private val getAttachmentIntentValues =
        GetAttachmentIntentValues(attachmentRepository, messageRepository)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns uri
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return intent values when attachment and metadata is locally available`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId
            )
        } returns messageAttachmentMetadata.right()
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns messageWithBody.right()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertEquals(OpenAttachmentIntentValues("application/pdf", uri).right(), result)
    }

    @Test
    fun `should return no data cached when attachment is not locally available`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId
            )
        } returns DataError.Local.NoDataCached.left()
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns messageWithBody.right()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertTrue(result.isLeft())
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return no data cached when message is not locally available`() = runTest {
        // Given
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns DataError.Local.NoDataCached.left()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertTrue(result.isLeft())
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return attachment repository error when getting attachment fails`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId
            )
        } returns DataError.Local.OutOfMemory.left()
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns messageWithBody.right()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.OutOfMemory.left(), result)
    }

    @Test
    fun `should return intent values when mime attachment is successfully saved and metadata is available`() = runTest {
        // Given
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns pgpMimeMessageWithBody.right()
        coEvery {
            attachmentRepository.saveMimeAttachmentToPublicStorage(
                userId = userId,
                messageId = messageId,
                attachmentId = AttachmentId("image")
            )
        } returns uri.right()

        // When
        val result = getAttachmentIntentValues(userId, messageId, AttachmentId("image"))

        // Then
        assertEquals(OpenAttachmentIntentValues("image/png", uri).right(), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `should return intent values with fixed mime-type when mime attachment is successfully saved, metadata is available and content-type is binary but file extension is well-known`() =
        runTest {
            // Given
            val messageWithBody = MessageWithBodySample.PgpMimeMessageWithPdfAttachmentWithBinaryContentType
            coEvery {
                messageRepository.getMessageWithBody(userId, messageId)
            } returns messageWithBody.right()
            coEvery {
                attachmentRepository.saveMimeAttachmentToPublicStorage(
                    userId = userId,
                    messageId = messageId,
                    attachmentId = AttachmentId("invoice_binary_content_type")
                )
            } returns uri.right()

            // When
            val result = getAttachmentIntentValues(userId, messageId, AttachmentId("invoice_binary_content_type"))

            println(result)

            // Then
            assertEquals(OpenAttachmentIntentValues("application/pdf", uri).right(), result)
        }

    @Test
    fun `should return error if saving mime attachment to public storage fails`() = runTest {
        // Given
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns pgpMimeMessageWithBody.right()
        coEvery {
            attachmentRepository.saveMimeAttachmentToPublicStorage(
                userId = userId,
                messageId = messageId,
                attachmentId = AttachmentId("image")
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = getAttachmentIntentValues(userId, messageId, AttachmentId("image"))

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }
}
