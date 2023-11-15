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

package ch.protonmail.android.mailmessage.data

import java.io.File
import android.net.Uri
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.local.usecase.AttachmentDecryptionError
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.mailmessage.data.repository.AttachmentRepositoryImpl
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttachmentRepositoryImplTest {


    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
    private val messageAttachment = MessageAttachmentSample.invoice
    private val attachmentMetaData = MessageAttachmentMetadata(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        uri = null,
        status = AttachmentWorkerStatus.Success
    )

    private val decryptAttachmentByteArray: DecryptAttachmentByteArray = mockk()
    private val localDataSource: AttachmentLocalDataSource = mockk()
    private val remoteDataSource: AttachmentRemoteDataSource = mockk()

    private val repository = AttachmentRepositoryImpl(
        decryptAttachmentByteArray,
        remoteDataSource,
        localDataSource,
        Dispatchers.Unconfined
    )

    @BeforeTest
    fun setUp() {
        mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")

        mockkStatic(Uri::class)
        coEvery { Uri.parse(any()) } returns mockk()
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
    }

    @Test
    fun `should return attachment from local data source if available`() = runTest {
        // Given
        coEvery { localDataSource.getAttachment(userId, messageId, attachmentId) } returns attachmentMetaData.right()

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(attachmentMetaData.right(), result)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should return attachment from remote data source if not available locally`() = runTest {
        // Given
        coEvery { localDataSource.getAttachment(userId, messageId, attachmentId) } returnsMany listOf(
            DataError.Local.NoDataCached.left(),
            attachmentMetaData.right()
        )
        coEvery {
            remoteDataSource.enqueueGetAttachmentWorker(userId, messageId, attachmentId)
        } returns Unit
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            attachmentMetaData
        )
        coEvery {
            localDataSource.updateAttachmentDownloadStatus(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = AttachmentWorkerStatus.Running
            )
        } just Runs

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(attachmentMetaData.right(), result)
        coVerify {
            localDataSource.updateAttachmentDownloadStatus(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = AttachmentWorkerStatus.Running
            )
        }
    }

    @Test
    fun `should return remote error when remote call fails`() = runTest {
        // Given
        coEvery {
            localDataSource.getAttachment(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery { remoteDataSource.enqueueGetAttachmentWorker(userId, messageId, attachmentId) } returns Unit
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            attachmentMetaData.copy(
                uri = null,
                status = AttachmentWorkerStatus.Failed.Generic
            )
        )
        coEvery {
            localDataSource.updateAttachmentDownloadStatus(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = AttachmentWorkerStatus.Running
            )
        } just Runs

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Remote.Unknown.left(), result)
    }

    @Test
    fun `should return not enough space error when worker fails due insufficient storage`() = runTest {
        // Given
        coEvery {
            localDataSource.getAttachment(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery { remoteDataSource.enqueueGetAttachmentWorker(userId, messageId, attachmentId) } returns Unit
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            attachmentMetaData.copy(
                uri = null,
                status = AttachmentWorkerStatus.Failed.OutOfMemory
            )
        )
        coEvery {
            localDataSource.updateAttachmentDownloadStatus(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = AttachmentWorkerStatus.Running
            )
        } just Runs

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.OutOfMemory.left(), result)
    }

    @Test
    fun `should return locally stored embedded image when it is locally available`() = runTest {
        // Given
        val encryptedByteArray = "I'm an encrypted embedded image".toByteArray()
        val expectedByteArray = "I'm an embedded image".toByteArray()

        val expectedFile = mockk<File> {
            every { readBytes() } returns encryptedByteArray
        }

        coEvery { localDataSource.getEmbeddedImage(userId, messageId, attachmentId) } returns expectedFile.right()
        coEvery {
            decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedByteArray)
        } returns expectedByteArray.right()

        // When
        val actual = repository.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(expectedByteArray.right(), actual)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should store and return remote fetched embedded image when it is locally not available`() = runTest {
        // Given
        val encryptedByteArray = "I'm an encrypted embedded image".toByteArray()
        val expectedByteArray = "I'm an embedded image".toByteArray()

        coEvery {
            localDataSource.getEmbeddedImage(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            remoteDataSource.getAttachment(userId, messageId, attachmentId)
        } returns encryptedByteArray.right()
        coEvery { localDataSource.storeEmbeddedImage(userId, messageId, attachmentId, encryptedByteArray) } just Runs
        coEvery {
            decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedByteArray)
        } returns expectedByteArray.right()

        // When
        val actual = repository.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(expectedByteArray.right(), actual)
        coVerify { localDataSource.storeEmbeddedImage(userId, messageId, attachmentId, encryptedByteArray) }
    }

    @Test
    fun `should return remote error when fetching embedded image failed`() = runTest {
        // Given
        val expected = DataError.Remote.Http(NetworkError.NoNetwork).left()

        coEvery {
            localDataSource.getEmbeddedImage(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            remoteDataSource.getAttachment(userId, messageId, attachmentId)
        } returns expected

        // When
        val actual = repository.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return decryption error when decrypting of an embedded image fails`() = runTest {
        // Given
        val encryptedByteArray = "I'm an encrypted embedded image".toByteArray()

        coEvery {
            localDataSource.getEmbeddedImage(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery {
            remoteDataSource.getAttachment(userId, messageId, attachmentId)
        } returns encryptedByteArray.right()
        coEvery {
            localDataSource.storeEmbeddedImage(userId, messageId, attachmentId, encryptedByteArray)
        } just runs
        coEvery {
            decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedByteArray)
        } returns AttachmentDecryptionError.DecryptionFailed.left()

        // When
        val actual = repository.getEmbeddedImage(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.DecryptionError.left(), actual)
    }

    @Test
    fun `observe local stored attachment metadata emits attachment metadata when existing`() = runTest {
        // Given
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            attachmentMetaData
        )

        // When
        repository.observeAttachmentMetadata(userId, messageId, attachmentId).test {
            // Then
            assertEquals(attachmentMetaData, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe local stored attachment metadata emits null when attachment metadata isn't stored`() = runTest {
        // Given
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            null
        )

        // When
        repository.observeAttachmentMetadata(userId, messageId, attachmentId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return local stored attachment metadata when status is running`() = runTest {
        // Given
        val attachment1 = buildMessageAttachmentMetadata(
            attachmentId = AttachmentId("attachment1"),
            messageId = MessageIdSample.Invoice
        )
        val attachment2 = buildMessageAttachmentMetadata(
            attachmentId = AttachmentId("attachment2"),
            messageId = MessageIdSample.Invoice
        )
        coEvery {
            localDataSource.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))
        } returns listOf(attachment1, attachment2)

        // When
        val result = repository.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertEquals(listOf(attachment1, attachment2), result)
    }

    @Test
    fun `should return empty list when locally no attachment metadata is in running state`() = runTest {
        // Given
        coEvery {
            localDataSource.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))
        } returns emptyList()

        // When
        val result = repository.getDownloadingAttachmentsForMessages(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `should call method to save attachment to local storage`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val expectedResult = Unit.right()
        coEvery { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) } returns expectedResult

        // When
        repository.saveAttachment(userId, messageId, attachmentId, uri)

        // Then
        coVerify { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) }
    }

    @Test
    fun `should return data error when call method to save attachment failed`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val expectedResult = DataError.Local.FailedToStoreFile.left()
        coEvery { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) } returns expectedResult

        // When
        val actual = repository.saveAttachment(userId, messageId, attachmentId, uri)

        // Then
        assertEquals(expectedResult, actual)
        coVerify { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) }
    }

    @Test
    fun `should return file from local storage when available`() = runTest {
        // Given
        val file = mockk<File>()
        coEvery { localDataSource.readFileFromStorage(userId, messageId, attachmentId) } returns file.right()

        // When
        val actual = repository.readFileFromStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(file.right(), actual)
        coVerify { localDataSource.readFileFromStorage(userId, messageId, attachmentId) }
    }

    @Test
    fun `should return local error when file from local storage is not available`() = runTest {
        // Given
        val expectedResult = DataError.Local.NoDataCached.left()
        coEvery { localDataSource.readFileFromStorage(userId, messageId, attachmentId) } returns expectedResult

        // When
        val actual = repository.readFileFromStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(expectedResult, actual)
        coVerify { localDataSource.readFileFromStorage(userId, messageId, attachmentId) }
    }

    @Test
    fun `should return attachment information from local when available`() = runTest {
        // Given
        val expected = messageAttachment.right()
        coEvery {
            localDataSource.getAttachmentInfo(userId, messageId, attachmentId)
        } returns expected

        // When
        val actual = repository.getAttachmentInfo(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should return local error when attachment information is not available`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        coEvery {
            localDataSource.getAttachmentInfo(userId, messageId, attachmentId)
        } returns expected

        // When
        val actual = repository.getAttachmentInfo(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should update attachment information in local storage`() = runTest {
        // Given
        val expected = Unit.right()
        coEvery {
            localDataSource.updateMessageAttachment(userId, messageId, attachmentId, messageAttachment)
        } returns expected

        // When
        val actual = repository.updateMessageAttachment(userId, messageId, attachmentId, messageAttachment)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should return local error when updating attachment information in local storage failed`() = runTest {
        // Given
        val expected = DataError.Local.FailedToStoreFile.left()
        coEvery {
            localDataSource.updateMessageAttachment(userId, messageId, attachmentId, messageAttachment)
        } returns expected

        // When
        val actual = repository.updateMessageAttachment(userId, messageId, attachmentId, messageAttachment)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should return file size from local storage`() = runTest {
        // Given
        val expected = 100L.right()
        val uri = mockk<Uri>()
        coEvery { localDataSource.getFileSizeFromUri(uri) } returns expected

        // When
        val actual = repository.getFileSizeFromUri(uri)

        // Then
        assertEquals(expected, actual)
        coVerify { localDataSource.getFileSizeFromUri(any()) }
    }

    @Test
    fun `should return local error when file size from local storage is not available`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        val uri = mockk<Uri>()
        coEvery { localDataSource.getFileSizeFromUri(uri) } returns expected

        // When
        val actual = repository.getFileSizeFromUri(uri)

        // Then
        assertEquals(expected, actual)
        coVerify { localDataSource.getFileSizeFromUri(any()) }
    }

    @Test
    fun `should return unit when upserting mime attachment was successful`() = runTest {
        // Given
        val attachmentContent = "attachmentContent".encodeToByteArray()
        val attachment = MessageAttachmentSample.embeddedImageAttachment
        coEvery {
            localDataSource.upsertMimeAttachment(userId, messageId, attachmentId, attachmentContent, attachment)
        } returns Unit.right()

        // When
        val actual = repository.saveMimeAttachment(userId, messageId, attachmentId, attachmentContent, attachment)

        // Then
        Assert.assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return a local error when upserting mime attachment has failed`() = runTest {
        // Given
        val attachmentContent = "attachmentContent".encodeToByteArray()
        val attachment = MessageAttachmentSample.embeddedImageAttachment
        coEvery {
            localDataSource.upsertMimeAttachment(userId, messageId, attachmentId, attachmentContent, attachment)
        } returns DataError.Local.FailedToStoreFile.left()

        // When
        val actual = repository.saveMimeAttachment(userId, messageId, attachmentId, attachmentContent, attachment)

        // Then
        Assert.assertEquals(DataError.Local.FailedToStoreFile.left(), actual)
    }

    @Test
    fun `should return Unit when copying mime attachments to message was successful`() = runTest {
        // Given
        val targetMessageId = MessageId(messageId.id + "_new")
        coEvery {
            localDataSource.copyMimeAttachmentsToMessage(
                userId = userId,
                sourceMessageId = messageId,
                targetMessageId = targetMessageId,
                attachmentIds = listOf(attachmentId)
            )
        } returns Unit.right()

        // When
        val actual = repository.copyMimeAttachmentsToMessage(
            userId = userId,
            sourceMessageId = messageId,
            targetMessageId = targetMessageId,
            attachmentIds = listOf(attachmentId)
        )

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 1) {
            localDataSource.copyMimeAttachmentsToMessage(
                userId = userId,
                sourceMessageId = messageId,
                targetMessageId = targetMessageId,
                attachmentIds = listOf(attachmentId)
            )
        }
    }

    @Test
    fun `should return local error when copying mime attachments to message has failed`() = runTest {
        // Given
        val targetMessageId = MessageId(messageId.id + "_new")
        coEvery {
            localDataSource.copyMimeAttachmentsToMessage(
                userId = userId,
                sourceMessageId = messageId,
                targetMessageId = targetMessageId,
                attachmentIds = listOf(attachmentId)
            )
        } returns DataError.Local.FailedToStoreFile.left()

        // When
        val actual = repository.copyMimeAttachmentsToMessage(
            userId = userId,
            sourceMessageId = messageId,
            targetMessageId = targetMessageId,
            attachmentIds = listOf(attachmentId)
        )

        // Then
        assertEquals(DataError.Local.FailedToStoreFile.left(), actual)
    }

    @Test
    fun `should return uri when saving mime attachment to public storage was successful`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery {
            localDataSource.saveMimeAttachmentToPublicStorage(userId, messageId, attachmentId)
        } returns uri.right()

        // When
        val result = repository.saveMimeAttachmentToPublicStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(uri.right(), result)
    }

    @Test
    fun `should return error when saving mime attachment to public storage failed`() = runTest {
        // Given
        coEvery {
            localDataSource.saveMimeAttachmentToPublicStorage(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = repository.saveMimeAttachmentToPublicStorage(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should return byte array when getting attachment from remote was successful`() = runTest {
        // Given
        val attachmentByteArray = "attachmentByteArray".encodeToByteArray()
        coEvery { remoteDataSource.getAttachment(userId, messageId, attachmentId) } returns attachmentByteArray.right()

        // When
        val result = repository.getAttachmentFromRemote(userId, messageId, attachmentId)

        // Then
        assertEquals(attachmentByteArray.right(), result)
    }

    @Test
    fun `should return error when getting attachment from remote has failed`() = runTest {
        // Given
        coEvery {
            remoteDataSource.getAttachment(userId, messageId, attachmentId)
        } returns DataError.Remote.Unknown.left()

        // When
        val result = repository.getAttachmentFromRemote(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Remote.Unknown.left(), result)
    }

    @Test
    fun `should return file when saving attachment byte array was successful`() = runTest {
        // Given
        val attachmentByteArray = "attachmentByteArray".encodeToByteArray()
        val file = mockk<File>()
        coEvery {
            localDataSource.saveAttachmentToFile(userId, messageId, attachmentId, attachmentByteArray)
        } returns file.right()

        // When
        val result = repository.saveAttachmentToFile(userId, messageId, attachmentId, attachmentByteArray)

        // Then
        assertEquals(file.right(), result)
    }

    @Test
    fun `should return error when saving attachment byte array has failed`() = runTest {
        // Given
        val attachmentByteArray = "attachmentByteArray".encodeToByteArray()
        coEvery {
            localDataSource.saveAttachmentToFile(userId, messageId, attachmentId, attachmentByteArray)
        } returns DataError.Local.FailedToStoreFile.left()

        // When
        val result = repository.saveAttachmentToFile(userId, messageId, attachmentId, attachmentByteArray)

        // Then
        assertEquals(DataError.Local.FailedToStoreFile.left(), result)
    }
}
