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

import java.io.File
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentMetadataDao
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataEntityTestData.buildMessageAttachmentMetadataEntity
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.util.kotlin.sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("BlockingMethodInNonBlockingContext")
class AttachmentLocalDataSourceImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
    private val file = File.createTempFile("test", "test")
    private val hash = file.sha256()

    private val messageAttachmentMetadataEntity = buildMessageAttachmentMetadataEntity(
        hash = hash,
        path = file.path
    )


    private val attachmentDao = mockk<MessageAttachmentMetadataDao>(relaxUnitFun = true)
    private val attachmentFileStorage = mockk<AttachmentFileStorage>()
    private val messageDatabase = mockk<MessageDatabase> {
        every { messageAttachmentMetadataDao() } returns attachmentDao
    }
    private val attachmentLocalDataSource = AttachmentLocalDataSourceImpl(
        db = messageDatabase,
        attachmentFileStorage = attachmentFileStorage,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun `should return file when metadata and file are stored locally and hashes are matching`() = runTest {
        // Given

        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity)
        coEvery {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        } returns file

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
    fun `should return null when metadata and file are stored locally and hashes do not matching`() = runTest {
        // Given
        val metaDataFile = File.createTempFile("metadata", "test")
        val file = File.createTempFile("test", "test")

        metaDataFile.outputStream().bufferedWriter().use { it.write("I'm a metadata content file") }
        file.outputStream().bufferedWriter().use { it.write("I'm a content file") }
        val attachmentId = AttachmentId("attachmentId")
        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity)
        coEvery {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        } returns file

        // When
        val result = attachmentLocalDataSource.getAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = AttachmentId("attachmentId")
        )

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return null when file is not stored locally`() = runTest {
        // Given
        coEvery {
            attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        } returns flowOf(messageAttachmentMetadataEntity)
        coEvery {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        } throws AttachmentFileReadException

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
    fun `should store attachment metadata locally when file storing was successful`() = runTest {
        // Given
        val attachmentContent = "I'm the content of a file"
        val attachmentContentByteArray = attachmentContent.toByteArray()
        val file = File.createTempFile("test", "test")
        file.outputStream().bufferedWriter().use { it.write(attachmentContent) }
        val attachmentToStore = MessageAttachmentMetadataEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            hash = file.sha256(),
            path = file.path,
            status = AttachmentWorkerStatus.Success
        )
        coEvery {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, attachmentContentByteArray)
        } returns file

        // When
        attachmentLocalDataSource.upsertAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            attachment = attachmentContentByteArray,
            status = AttachmentWorkerStatus.Success
        )

        // Then
        coVerifyOrder {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, attachmentContentByteArray)
            attachmentDao.insertOrUpdate(attachmentToStore)
        }
    }

    @Test
    fun `should not store attachment metadata when file storing was not successful`() = runTest {
        // Given
        val attachmentContent = "I'm the content of a file"
        val attachmentContentByteArray = attachmentContent.toByteArray()
        coEvery {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, attachmentContentByteArray)
        } returns null

        // When
        attachmentLocalDataSource.upsertAttachment(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            attachment = attachmentContentByteArray,
            status = AttachmentWorkerStatus.Failed
        )

        // Then
        coVerify {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, attachmentContentByteArray)
        }
        verify { attachmentDao wasNot Called }
    }

    @Test
    fun `should return true when deleting attachment files and metadata from db was successful`() = runTest {
        // Given
        coEvery { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) } returns Unit
        coEvery { attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId.id) } returns true

        // When
        val result = attachmentLocalDataSource.deleteAttachments(userId, messageId)

        // Then
        assertTrue(result)
        coVerify { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) }
        coVerify { attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId.id) }
    }

    @Test
    fun `should return false when deleting attachment files and metadata from db failed`() = runTest {
        // Given
        coEvery { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) } returns Unit
        coEvery { attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId.id) } returns false

        // When
        val result = attachmentLocalDataSource.deleteAttachments(userId, messageId)

        // Then
        assertFalse(result)
        coVerify { attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId) }
        coVerify { attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId.id) }
    }

    @Test
    fun `should return metadata when it is found by hash`() = runTest {
        // Given
        coEvery { attachmentDao.getMessageAttachmentMetadataByHash(hash) } returns messageAttachmentMetadataEntity

        // When
        val result = attachmentLocalDataSource.getAttachmentMetadataByHash(hash)

        // Then
        assertEquals(messageAttachmentMetadataEntity.toMessageAttachmentMetadata().right(), result)
    }

    @Test
    fun `should return no data cached when metadata is not found by hash`() = runTest {
        // Given
        coEvery { attachmentDao.getMessageAttachmentMetadataByHash(hash) } returns null

        // When
        val result = attachmentLocalDataSource.getAttachmentMetadataByHash(hash)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return list of metadata when multiple attachments are downloading for a user`() = runTest {
        // Given
        val attachment1 = buildMessageAttachmentMetadataEntity(
            attachmentId = AttachmentId("attachmentId1"),
            messageId = MessageIdSample.Invoice
        )
        val attachment2 = buildMessageAttachmentMetadataEntity(
            attachmentId = AttachmentId("attachmentId2"),
            messageId = MessageIdSample.Invoice
        )
        val attachment3 = buildMessageAttachmentMetadataEntity(
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
