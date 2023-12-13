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

package ch.protonmail.android.composer.data.usecase

import java.io.File
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.ApiMessageId
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.Attachment1
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.Attachment2
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.AttachmentId1
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.AttachmentId2
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.AttachmentIds
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.DraftState
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.MessageId
import ch.protonmail.android.composer.data.usecase.GetAttachmentFilesTest.TestData.UserId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.data.local.usecase.AttachmentDecryptionError
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAttachmentFilesTest {

    private val attachmentRepository = mockk<AttachmentRepository> {
        coEvery { readFileFromStorage(UserId, ApiMessageId, AttachmentId1) } returns Attachment1.right()
    }
    private val decryptAttachmentByteArray = mockk<DecryptAttachmentByteArray>()
    private val draftStateRepository = mockk<DraftStateRepository> {
        coEvery { observe(UserId, MessageId) } returns flowOf(DraftState.right())
    }

    private val getAttachmentFiles = GetAttachmentFiles(
        attachmentRepository = attachmentRepository,
        decryptAttachmentByteArray = decryptAttachmentByteArray,
        draftStateRepository = draftStateRepository
    )

    @Test
    fun `should return files if all attachments were read successfully`() = runTest {
        // Given
        val expected = mapOf(AttachmentId1 to Attachment1, AttachmentId2 to Attachment2).right()
        expectReadFileFromStorageSucceeds(AttachmentId2, Attachment2)

        // When
        val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return files when some attachments are not available locally and are fetched from api successfully`() =
        runTest {
            // Given
            val encryptedAttachmentByteArray = "encryptedAttachmentByteArray".encodeToByteArray()
            val decryptedAttachmentByteArray = "decryptedAttachmentByteArray".encodeToByteArray()
            expectReadFileFromStorageFails(AttachmentId2)
            expectGetAttachmentFromRemoteSucceeds(AttachmentId2, encryptedAttachmentByteArray)
            expectDecryptAttachmentByteArraySucceeds(
                AttachmentId2, encryptedAttachmentByteArray, decryptedAttachmentByteArray
            )
            expectSaveAttachmentSucceeds(AttachmentId2, decryptedAttachmentByteArray, Attachment2)

            // When
            val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

            // Then
            val expected = mapOf(AttachmentId1 to Attachment1, AttachmentId2 to Attachment2).right()
            assertEquals(expected, actual)
            coVerifyOrder {
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId1)
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId2)
                attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, AttachmentId2)
                decryptAttachmentByteArray(UserId, ApiMessageId, AttachmentId2, encryptedAttachmentByteArray)
                attachmentRepository.saveAttachmentToFile(
                    UserId, ApiMessageId, AttachmentId2, decryptedAttachmentByteArray
                )
            }
        }

    @Test
    fun `should return error when some attachments are not available locally and fetching them from api has failed`() =
        runTest {
            // Given
            expectReadFileFromStorageFails(AttachmentId2)
            coEvery {
                attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, AttachmentId2)
            } returns DataError.Remote.Unknown.left()

            // When
            val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

            // Then
            assertEquals(GetAttachmentFiles.Error.DownloadingAttachments.left(), actual)
            coVerifyOrder {
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId1)
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId2)
                attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, AttachmentId2)
            }
        }

    @Test
    fun `should return error when attachments not available locally are fetched but the decryption failed`() = runTest {
        // Given
        val encryptedAttachmentByteArray = "encryptedAttachmentByteArray".encodeToByteArray()
        expectReadFileFromStorageFails(AttachmentId2)
        expectGetAttachmentFromRemoteSucceeds(AttachmentId2, encryptedAttachmentByteArray)
        coEvery {
            decryptAttachmentByteArray(UserId, ApiMessageId, AttachmentId2, encryptedAttachmentByteArray)
        } returns AttachmentDecryptionError.DecryptionFailed.left()

        // When
        val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(GetAttachmentFiles.Error.DownloadingAttachments.left(), actual)
        coVerifyOrder {
            attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId1)
            attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId2)
            attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, AttachmentId2)
            decryptAttachmentByteArray(UserId, ApiMessageId, AttachmentId2, encryptedAttachmentByteArray)
        }
    }

    @Test
    fun `should return error when attachments not available locally are fetched and decrypted but saving failed`() =
        runTest {
            // Given
            val encryptedAttachmentByteArray = "encryptedAttachmentByteArray".encodeToByteArray()
            val decryptedAttachmentByteArray = "decryptedAttachmentByteArray".encodeToByteArray()
            expectReadFileFromStorageFails(AttachmentId2)
            expectGetAttachmentFromRemoteSucceeds(AttachmentId2, encryptedAttachmentByteArray)
            expectDecryptAttachmentByteArraySucceeds(
                AttachmentId2, encryptedAttachmentByteArray, decryptedAttachmentByteArray
            )
            coEvery {
                attachmentRepository.saveAttachmentToFile(
                    UserId, ApiMessageId, AttachmentId2, decryptedAttachmentByteArray
                )
            } returns DataError.Local.FailedToStoreFile.left()

            // When
            val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

            // Then
            assertEquals(GetAttachmentFiles.Error.FailedToStoreFile.left(), actual)
            coVerifyOrder {
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId1)
                attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId2)
                attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, AttachmentId2)
                decryptAttachmentByteArray(UserId, ApiMessageId, AttachmentId2, encryptedAttachmentByteArray)
                attachmentRepository.saveAttachmentToFile(
                    UserId, ApiMessageId, AttachmentId2, decryptedAttachmentByteArray
                )
            }
        }

    @Test
    fun `should return error if api assigned message id still doesn't exist`() = runTest {
        // Given
        val expected = GetAttachmentFiles.Error.DraftNotFound.left()
        coEvery { draftStateRepository.observe(UserId, MessageId) } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val actual = getAttachmentFiles(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(expected, actual)
    }

    private fun expectReadFileFromStorageSucceeds(attachmentId: AttachmentId, attachment: File) {
        coEvery {
            attachmentRepository.readFileFromStorage(UserId, ApiMessageId, attachmentId)
        } returns attachment.right()
    }

    private fun expectReadFileFromStorageFails(attachmentId: AttachmentId) {
        coEvery {
            attachmentRepository.readFileFromStorage(UserId, ApiMessageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectGetAttachmentFromRemoteSucceeds(
        attachmentId: AttachmentId,
        encryptedAttachmentContent: ByteArray
    ) {
        coEvery {
            attachmentRepository.getAttachmentFromRemote(UserId, ApiMessageId, attachmentId)
        } returns encryptedAttachmentContent.right()
    }

    private fun expectDecryptAttachmentByteArraySucceeds(
        attachmentId: AttachmentId,
        encryptedAttachmentContent: ByteArray,
        decryptedAttachmentContent: ByteArray
    ) {
        coEvery {
            decryptAttachmentByteArray(UserId, ApiMessageId, attachmentId, encryptedAttachmentContent)
        } returns decryptedAttachmentContent.right()
    }

    private fun expectSaveAttachmentSucceeds(
        attachmentId: AttachmentId,
        decryptedAttachmentContent: ByteArray,
        attachment: File
    ) {
        coEvery {
            attachmentRepository.saveAttachmentToFile(UserId, ApiMessageId, attachmentId, decryptedAttachmentContent)
        } returns attachment.right()
    }

    object TestData {
        val UserId = UserIdTestData.userId
        val MessageId = MessageIdSample.MessageWithAttachments
        val ApiMessageId = MessageId("apiMessageId")

        val AttachmentId1 = AttachmentId("attachmentId1")
        val AttachmentId2 = AttachmentId("attachmentId2")
        val AttachmentIds = listOf(AttachmentId1, AttachmentId2)

        val Attachment1 = File.createTempFile("attachment1", "txt")
        val Attachment2 = File.createTempFile("attachment2", "txt")

        val DraftState = DraftState(
            UserId,
            MessageId,
            ApiMessageId,
            DraftSyncState.Synchronized,
            DraftAction.Compose,
            null,
            false
        )
    }
}
