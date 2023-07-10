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

package ch.protonmail.android.mailmessage.data.local

import java.io.InputStream
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentMetadataDao
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.local.usecase.AttachmentDecryptionError
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.local.usecase.PrepareAttachmentForSharing
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataEntityTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachmentLocalDataSourceImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")

    private val mockUri = mockk<Uri>()
    private val inputStream = mockk<InputStream> {
        every { close() } just Runs
    }
    private val mockContentResolver = mockk<ContentResolver> {
        every { openInputStream(any()) } returns inputStream
    }
    private val context = mockk<Context> {
        every { contentResolver } returns mockContentResolver
    }

    private val messageAttachmentMetadataEntity = MessageAttachmentMetadataEntity(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        uri = mockUri,
        status = AttachmentWorkerStatus.Running
    )

    private val attachmentDao = mockk<MessageAttachmentMetadataDao>(relaxUnitFun = true)
    private val messageDatabase = mockk<MessageDatabase> {
        every { messageAttachmentMetadataDao() } returns attachmentDao
    }
    private val decryptAttachmentByteArray = mockk<DecryptAttachmentByteArray>()
    private val prepareAttachmentForSharing = mockk<PrepareAttachmentForSharing>()

    private val attachmentLocalDataSource = AttachmentLocalDataSourceImpl(
        db = messageDatabase,
        context = context,
        decryptAttachmentByteArray = decryptAttachmentByteArray,
        prepareAttachmentForSharing = prepareAttachmentForSharing,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return attachment metadata when file is stored locally`() = runTest {
        // Given
        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity)

        // When
        val result = attachmentLocalDataSource.getAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId
        )

        // Then
        assertEquals(messageAttachmentMetadataEntity.toMessageAttachmentMetadata().right(), result)
    }

    @Test
    fun `should return null when uri is not stored`() = runTest {
        // Given
        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity.copy(uri = null))

        // When
        val result = attachmentLocalDataSource.getAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId
        )

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return null when uri is stored but file doesn't exist`() = runTest {
        // Given
        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity)
        every { mockContentResolver.openInputStream(mockUri) } returns null

        // When
        val result = attachmentLocalDataSource.getAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId
        )

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should store attachment metadata locally when file decryption and storing was successful`() = runTest {
        // Given
        val encryptedAttachmentContent = "I'm the ecnrypted content of a file"
        val encryptedAttachmentContentByteArray = encryptedAttachmentContent.toByteArray()
        val decryptedAttachmentContentByteArray = "I'm the decrypted content of a file".toByteArray()

        coEvery {
            decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedAttachmentContentByteArray)
        } returns decryptedAttachmentContentByteArray.right()
        coEvery { prepareAttachmentForSharing(userId, messageId, attachmentId, any()) } returns mockUri.right()

        val attachmentToStore = MessageAttachmentMetadataEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            uri = mockUri,
            status = AttachmentWorkerStatus.Success
        )

        // When
        attachmentLocalDataSource.upsertAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            encryptedAttachment = encryptedAttachmentContentByteArray,
            status = AttachmentWorkerStatus.Success
        )

        // Then
        coVerify { attachmentDao.insertOrUpdate(attachmentToStore) }
    }

    @Test
    fun `should update attachment metadata when file decryption failed`() = runTest {
        // Given
        val encryptedAttachmentContent = "I'm the ecnrypted content of a file"
        val encryptedAttachmentContentByteArray = encryptedAttachmentContent.toByteArray()

        coEvery {
            decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedAttachmentContentByteArray)
        } returns AttachmentDecryptionError.DecryptionFailed.left()

        val attachmentToStore = MessageAttachmentMetadataEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            uri = null,
            status = AttachmentWorkerStatus.Failed
        )

        // When
        attachmentLocalDataSource.upsertAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            encryptedAttachment = encryptedAttachmentContentByteArray,
            status = AttachmentWorkerStatus.Success
        )

        // Then
        coVerify { attachmentDao.insertOrUpdate(attachmentToStore) }
    }

    @Test
    fun `should return true when deleting attachment files and metadata from db was successful`() = runTest {
        // Given
        coEvery { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) } returns Unit

        // When
        val result = attachmentLocalDataSource.deleteAttachments(userId, messageId)

        // Then
        assertTrue(result)
        coVerify { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) }
    }

    @Test
    fun `should return list of metadata when multiple attachments are downloading for a user`() = runTest {
        // Given
        val attachment1 = MessageAttachmentMetadataEntityTestData.buildMessageAttachmentMetadataEntity(
            attachmentId = AttachmentId("attachmentId1"),
            messageId = MessageIdSample.Invoice
        )
        val attachment2 = MessageAttachmentMetadataEntityTestData.buildMessageAttachmentMetadataEntity(
            attachmentId = AttachmentId("attachmentId2"),
            messageId = MessageIdSample.Invoice
        )
        val attachment3 = MessageAttachmentMetadataEntityTestData.buildMessageAttachmentMetadataEntity(
            attachmentId = AttachmentId("attachmentId3"),
            messageId = MessageIdSample.Invoice
        )

        coEvery {
            attachmentDao.getAttachmentsForUserMessagesAndStatus(
                userId, listOf(MessageIdSample.Invoice), AttachmentWorkerStatus.Running
            )
        } returns listOf(attachment1, attachment2, attachment3)

        // When
        val result =
            attachmentLocalDataSource.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertEquals(
            listOf(
                attachment1.toMessageAttachmentMetadata(),
                attachment2.toMessageAttachmentMetadata(),
                attachment3.toMessageAttachmentMetadata()
            ),
            result
        )
    }

    @Test
    fun `should return empty list when no attachments are downloading for a user`() = runTest {
        // Given
        coEvery {
            attachmentDao.getAttachmentsForUserMessagesAndStatus(
                userId, listOf(MessageIdSample.Invoice), AttachmentWorkerStatus.Running
            )
        } returns emptyList()

        // When
        val result =
            attachmentLocalDataSource.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertTrue(result.isEmpty())
    }
}
