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

import android.content.Context
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.AttachmentFileUriProvider
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetAttachmentIntentValuesTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("invoice")

    private val hash = "hash"
    private val extension = "txt"
    private val uri = mockk<Uri>()

    private val messageAttachmentMetadata = MessageAttachmentMetadata(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        hash = hash,
        path = "/test/tmp.$extension",
        status = AttachmentWorkerStatus.Success
    )
    private val messageWithBody = MessageWithBody(
        message = MessageTestData.message,
        messageBody = MessageBodyTestData.messageBodyWithAttachment
    )

    private val context = mockk<Context> {
        every { packageName } returns "test-package"
    }
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val messageRepository = mockk<MessageRepository>()
    private val fileUriProvider = mockk<AttachmentFileUriProvider>()

    private val getAttachmentIntentValues =
        GetAttachmentIntentValues(context, attachmentRepository, messageRepository, fileUriProvider)

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
        coEvery { fileUriProvider.getAttachmentFileUri(context, hash, "pdf") } returns uri

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertTrue(result.isRight())
        assertNotNull(result.getOrNull())
        val values = result.getOrNull()!!
        assertEquals(uri, values.uri)
        assertEquals(messageWithBody.messageBody.attachments.first().mimeType, values.mimeType)
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
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId
            )
        } returns messageAttachmentMetadata.right()
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns DataError.Local.NoDataCached.left()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertTrue(result.isLeft())
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return no data cached when attachment hash is not stored in metadata`() = runTest {
        // Given
        coEvery {
            attachmentRepository.getAttachment(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId
            )
        } returns messageAttachmentMetadata.copy(hash = null).right()
        coEvery { messageRepository.getMessageWithBody(userId, messageId) } returns messageWithBody.right()

        // When
        val result = getAttachmentIntentValues(userId, messageId, attachmentId)

        // Then
        assertTrue(result.isLeft())
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

}
