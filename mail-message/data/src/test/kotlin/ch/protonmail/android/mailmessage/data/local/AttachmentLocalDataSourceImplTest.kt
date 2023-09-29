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
import java.io.InputStream
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.file.FileInformation
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentMetadataDao
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.local.usecase.AttachmentDecryptionError
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.local.usecase.PrepareAttachmentForSharing
import ch.protonmail.android.mailmessage.data.mapper.MessageAttachmentEntityMapper
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentEntityTestData
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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

    private val attachmentFileStorage = mockk<AttachmentFileStorage>()
    private val attachmentDao = mockk<MessageAttachmentDao>(relaxUnitFun = true)
    private val attachmentMetadataDao = mockk<MessageAttachmentMetadataDao>(relaxUnitFun = true)
    private val messageDatabase = mockk<MessageDatabase> {
        every { messageAttachmentDao() } returns attachmentDao
        every { messageAttachmentMetadataDao() } returns attachmentMetadataDao
    }
    private val decryptAttachmentByteArray = mockk<DecryptAttachmentByteArray>()
    private val prepareAttachmentForSharing = mockk<PrepareAttachmentForSharing>()

    private val attachmentLocalDataSource = AttachmentLocalDataSourceImpl(
        db = messageDatabase,
        attachmentFileStorage = attachmentFileStorage,
        context = context,
        decryptAttachmentByteArray = decryptAttachmentByteArray,
        prepareAttachmentForSharing = prepareAttachmentForSharing,
        messageAttachmentEntityMapper = MessageAttachmentEntityMapper(),
        ioDispatcher = Dispatchers.Unconfined
    )

    @BeforeTest
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return attachment metadata when file is stored locally`() = runTest {
        // Given
        coEvery {
            attachmentMetadataDao.observeAttachmentMetadata(userId, messageId, attachmentId)
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
            attachmentMetadataDao.observeAttachmentMetadata(userId, messageId, attachmentId)
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
            attachmentMetadataDao.observeAttachmentMetadata(userId, messageId, attachmentId)
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
        coVerify { attachmentMetadataDao.insertOrUpdate(attachmentToStore) }
    }

    @Test
    fun `should store attachment metadata locally when saving the file to internal storage was successful`() = runTest {
        // Given
        val expectedResult = Unit.right()
        val fileName = "name"
        val fileSize = 123L
        val fileMimeType = "mimeType"
        val messageAttachmentEntity = MessageAttachmentEntityTestData.build(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            name = fileName,
            size = fileSize,
            mimeType = fileMimeType,
            disposition = "attachment"
        )
        coEvery {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, mockUri)
        } returns FileInformation(fileName, fileSize, fileMimeType)

        // When
        val actual = attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, mockUri)

        // Then
        assertEquals(expectedResult, actual)
        coVerify { attachmentDao.insertOrUpdate(messageAttachmentEntity) }
    }

    @Test
    fun `should not store attachment metadata locally when saving the file to internal storage has failed`() = runTest {
        // Given
        val expectedResult = DataError.Local.FailedToStoreFile.left()
        coEvery {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, mockUri)
        } returns null

        // When
        val actual = attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, mockUri)

        // Then
        assertEquals(expectedResult, actual)
        coVerify(exactly = 0) { attachmentDao.insertOrUpdate(any()) }
    }

    @Test
    fun `should return data error when storing attachment metadata locally has failed`() = runTest {
        // Given
        val expectedResult = DataError.Local.FailedToStoreFile.left()
        val fileName = "name"
        val fileSize = 123L
        val fileMimeType = "mimeType"
        val messageAttachmentEntity = MessageAttachmentEntityTestData.build(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            name = fileName,
            size = fileSize,
            mimeType = fileMimeType,
            disposition = "attachment"
        )
        coEvery {
            attachmentFileStorage.saveAttachment(userId, messageId.id, attachmentId.id, mockUri)
        } returns FileInformation(fileName, fileSize, fileMimeType)
        coEvery { attachmentDao.insertOrUpdate(any()) } throws Exception()

        // When
        val actual = attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, mockUri)

        // Then
        assertEquals(expectedResult, actual)
        coVerify { attachmentDao.insertOrUpdate(messageAttachmentEntity) }
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
            status = AttachmentWorkerStatus.Failed.Generic
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
        coVerify { attachmentMetadataDao.insertOrUpdate(attachmentToStore) }
    }

    @Test
    fun `should return true when deleting attachment files and metadata from db was successful`() = runTest {
        // Given
        coEvery { attachmentMetadataDao.deleteAttachmentMetadataForMessage(userId, messageId) } returns Unit

        // When
        val result = attachmentLocalDataSource.deleteAttachments(userId, messageId)

        // Then
        assertTrue(result)
        coVerify { attachmentMetadataDao.deleteAttachmentMetadataForMessage(userId, messageId) }
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
            attachmentMetadataDao.getAttachmentsForUserMessagesAndStatus(
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
            attachmentMetadataDao.getAttachmentsForUserMessagesAndStatus(
                userId, listOf(MessageIdSample.Invoice), AttachmentWorkerStatus.Running
            )
        } returns emptyList()

        // When
        val result =
            attachmentLocalDataSource.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return file when getting embedded image call was successful`() = runTest {
        // Given
        @Suppress("BlockingMethodInNonBlockingContext")
        val file = File.createTempFile("test", "test")
        coEvery { attachmentFileStorage.readCachedAttachment(userId, messageId.id, attachmentId.id) } returns file
        val expected = file.right()

        // When
        val actual = attachmentLocalDataSource.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return local error when getting embedded image call has failed`() = runTest {
        // Given
        coEvery {
            attachmentFileStorage.readCachedAttachment(userId, messageId.id, attachmentId.id)
        } throws AttachmentFileReadException
        val expected = DataError.Local.NoDataCached.left()

        // When
        val actual = attachmentLocalDataSource.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `verify file is stored in attachment file storage when saving an embedded image`() = runTest {
        // Given
        @Suppress("BlockingMethodInNonBlockingContext")
        val file = File.createTempFile("test", "test")
        val byteArray = file.readBytes()
        coEvery {
            attachmentFileStorage.saveAttachmentCached(userId, messageId.id, attachmentId.id, byteArray)
        } returns file

        // When
        attachmentLocalDataSource.storeEmbeddedImage(userId, messageId, attachmentId, byteArray)

        // Then
        coVerify { attachmentFileStorage.saveAttachmentCached(userId, messageId.id, attachmentId.id, byteArray) }
    }

    @Test
    fun `read file from storage returns stored file`() = runTest {
        // Given
        @Suppress("BlockingMethodInNonBlockingContext")
        val file = File.createTempFile("test", "test")
        coEvery {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        } returns file

        // When
        val result = attachmentLocalDataSource.readFileFromStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(file.right(), result)
    }

    @Test
    fun `read file from storage returns null when file doesn't exist`() = runTest {
        // Given
        coEvery {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        } throws AttachmentFileReadException

        // When
        val result = attachmentLocalDataSource.readFileFromStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `get attachment info returns stored information`() = runTest {
        // Given
        coEvery {
            attachmentDao.getMessageAttachment(userId, messageId, attachmentId)
        } returns MessageAttachmentEntityTestData.invoice()
        val expected = MessageAttachmentSample.invoice

        // When
        val actual = attachmentLocalDataSource.getAttachmentInfo(userId, messageId, attachmentId)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `get attachment info return no data cached when attachment doesn't exist`() = runTest {
        // Given
        coEvery {
            attachmentDao.getMessageAttachment(userId, messageId, attachmentId)
        } returns null

        // When
        val actual = attachmentLocalDataSource.getAttachmentInfo(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `update message attachment updates attachment in db and attachment file name in app storage`() = runTest {
        // Given
        val expectedAttachmentId = AttachmentId("updated_attachmentId")
        val keyPackets = "updated_keyPackets"
        val attachment = MessageAttachmentSample.invoice.copy(
            attachmentId = expectedAttachmentId,
            keyPackets = keyPackets
        )
        coEvery {
            attachmentDao.updateAttachmentIdAndKeyPackets(
                userId = userId,
                messageId = messageId,
                localAttachmentId = attachmentId,
                apiAssignedId = expectedAttachmentId,
                keyPackets = keyPackets
            )
        } just Runs
        coEvery {
            attachmentFileStorage.updateFileNameForAttachment(
                userId = userId,
                messageId = messageId.id,
                oldAttachmentId = attachmentId.id,
                newAttachmentId = expectedAttachmentId.id
            )
        } just Runs

        // When
        val actual = attachmentLocalDataSource.updateMessageAttachment(
            userId,
            messageId,
            attachmentId,
            attachment
        )

        // Then
        assertEquals(Unit.right(), actual)
        coVerify {
            attachmentDao.updateAttachmentIdAndKeyPackets(
                userId,
                messageId,
                attachmentId,
                expectedAttachmentId,
                keyPackets
            )
        }
        coVerify {
            attachmentFileStorage.updateFileNameForAttachment(
                userId = userId,
                messageId = messageId.id,
                oldAttachmentId = attachmentId.id,
                newAttachmentId = expectedAttachmentId.id
            )
        }
    }

    @Test
    fun `update message attachment returns unknown error when update fails`() = runTest {
        // Given
        val expectedAttachmentId = AttachmentId("updated_attachmentId")
        val keyPackets = "updated_keyPackets"
        val attachment = MessageAttachmentSample.invoice.copy(
            attachmentId = expectedAttachmentId,
            keyPackets = keyPackets
        )
        coEvery {
            attachmentDao.updateAttachmentIdAndKeyPackets(
                userId,
                messageId,
                attachmentId,
                expectedAttachmentId,
                keyPackets
            )
        } throws Exception()

        // When
        val actual = attachmentLocalDataSource.updateMessageAttachment(
            userId,
            messageId,
            attachmentId,
            attachment
        )

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }
}
