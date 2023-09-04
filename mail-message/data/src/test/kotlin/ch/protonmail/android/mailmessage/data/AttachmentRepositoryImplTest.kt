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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttachmentRepositoryImplTest {


    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
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
            remoteDataSource.getAttachment(userId, messageId, attachmentId)
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
        coEvery { remoteDataSource.getAttachment(userId, messageId, attachmentId) } returns Unit
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
        coEvery { remoteDataSource.getAttachment(userId, messageId, attachmentId) } returns Unit
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
            remoteDataSource.getEmbeddedImage(userId, messageId, attachmentId)
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
            remoteDataSource.getEmbeddedImage(userId, messageId, attachmentId)
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
            remoteDataSource.getEmbeddedImage(userId, messageId, attachmentId)
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
        coEvery { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) } just runs

        // When
        repository.saveAttachment(userId, messageId, attachmentId, uri)

        // Then
        coVerify { localDataSource.upsertAttachment(userId, messageId, attachmentId, uri) }
    }
}
