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

package ch.protonmail.android.composer.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSource
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.sample.AttachmentStateSample
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentRepositoryImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.MessageWithAttachments
    private val attachmentId = AttachmentId("attachmentId")
    private val fileName = "fileName.txt"
    private val mimeType = "text/plain"
    private val byteContent = "Content of a text file".toByteArray()

    private val attachmentStateLocalDataSource = mockk<AttachmentStateLocalDataSource>()
    private val attachmentRemoteDataSource = mockk<AttachmentRemoteDataSource> {
        coJustRun { deleteAttachmentFromDraft(userId, attachmentId) }
    }
    private val attachmentLocalDataSource = mockk<AttachmentLocalDataSource>()

    private val attachmentRepositoryImpl = AttachmentRepositoryImpl(
        attachmentStateLocalDataSource,
        attachmentRemoteDataSource,
        attachmentLocalDataSource
    )

    @Test
    fun `when uploaded attachment is deleted then remote layer is called before delete with file locally is called`() =
        runTest {
            // Given
            val expected = Unit.right()
            expectLoadingAttachmentStateLoadsUploadedStateSuccessful()
            expectDeleteAttachmentWithFileLocalSuccessful()

            // When
            val actual = attachmentRepositoryImpl.deleteAttachment(userId, messageId, attachmentId)

            // Then
            assertEquals(expected, actual)
            coVerifyOrder {
                attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
                attachmentRemoteDataSource.deleteAttachmentFromDraft(userId, attachmentId)
                attachmentLocalDataSource.deleteAttachmentWithFile(userId, messageId, attachmentId)
            }
        }

    @Test
    fun `when parent uploaded attachment is deleted then remote layer is executed before delete locally is called`() =
        runTest {
            // Given
            val expected = Unit.right()
            expectLoadingAttachmentStateLoadsParentUploadedStateSuccessful()
            expectDeleteAttachmentLocalSuccessful()

            // When
            val actual = attachmentRepositoryImpl.deleteAttachment(userId, messageId, attachmentId)

            // Then
            assertEquals(expected, actual)
            coVerifyOrder {
                attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
                attachmentRemoteDataSource.deleteAttachmentFromDraft(userId, attachmentId)
                attachmentLocalDataSource.deleteAttachment(userId, messageId, attachmentId)
            }
        }

    @Test
    fun `deleting file cancel worker when attachment state is not uploaded`() = runTest {
        // Given
        val expected = Unit.right()
        expectLoadingAttachmentStateLoadsLocalStateSuccessful()
        expectCancelAttachmentWorkerSucceeds()
        expectDeleteAttachmentWithFileLocalSuccessful()

        // When
        val actual = attachmentRepositoryImpl.deleteAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerifyOrder {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
            attachmentRemoteDataSource.cancelAttachmentUpload(attachmentId)
            attachmentLocalDataSource.deleteAttachmentWithFile(userId, messageId, attachmentId)
        }
    }

    @Test
    fun `returns local error when deleting file locally fails`() = runTest {
        // Given
        val expected = DataError.Local.FailedToDeleteFile.left()
        expectLoadingAttachmentStateLoadsUploadedStateSuccessful()
        expectDeleteAttachmentLocalFailed()

        // When
        val actual = attachmentRepositoryImpl.deleteAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerifyOrder {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
            attachmentRemoteDataSource.deleteAttachmentFromDraft(userId, attachmentId)
            attachmentLocalDataSource.deleteAttachmentWithFile(userId, messageId, attachmentId)
        }
    }

    @Test
    fun `returns local error when attachment state is not found`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        expectLoadingAttachmentStateFailed()

        // When
        val actual = attachmentRepositoryImpl.deleteAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerify { attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId) }
        coVerify { attachmentRemoteDataSource wasNot Called }
        coVerify { attachmentLocalDataSource wasNot Called }
    }

    @Test
    fun `returns local error when storing file locally fails`() = runTest {
        // Given
        val expected = DataError.Local.FailedToStoreFile.left()
        expectUpsertAttachmentLocalFailed()

        // When
        val actual = attachmentRepositoryImpl.createAttachment(
            userId,
            messageId,
            attachmentId,
            fileName,
            mimeType,
            byteContent
        )

        // Then
        assertEquals(expected, actual)
        coVerifyOrder {
            attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, fileName, mimeType, byteContent)
        }
    }

    @Test
    fun `returns local error when createOrUpdate AttachmentState failed`() = runTest {
        // Given
        expectUpsertAttachmentLocalSuccessful()
        val expectedAttachmentState = AttachmentStateSample.build(
            userId,
            messageId,
            attachmentId,
            AttachmentSyncState.Local
        )
        expectCreateOrUpdateAttachmentStateFailed(expectedAttachmentState)

        // When
        val actual = attachmentRepositoryImpl.createAttachment(
            userId,
            messageId,
            attachmentId,
            fileName,
            mimeType,
            byteContent
        )

        // Then
        assert(actual.isLeft())
        assert(actual.leftOrNull() is DataError.Local)
        coVerifyOrder {
            attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, fileName, mimeType, byteContent)
            attachmentStateLocalDataSource.createOrUpdate(expectedAttachmentState)
        }
    }

    @Test
    fun `when creating Attachment, stores a file on disk and creates corresponding AttachmentState`() = runTest {
        // Given
        expectUpsertAttachmentLocalSuccessful()
        val expectedAttachmentState = AttachmentStateSample.build(
            userId,
            messageId,
            attachmentId,
            AttachmentSyncState.Local
        )
        expectCreateOrUpdateAttachmentStateSuccessful(expectedAttachmentState)

        // When
        val actual = attachmentRepositoryImpl.createAttachment(
            userId,
            messageId,
            attachmentId,
            fileName,
            mimeType,
            byteContent
        )

        // Then
        assertEquals(Unit.right(), actual)
        coVerifyOrder {
            attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, fileName, mimeType, byteContent)
            attachmentStateLocalDataSource.createOrUpdate(expectedAttachmentState)
        }
    }


    private fun expectLoadingAttachmentStateLoadsUploadedStateSuccessful() {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns AttachmentStateSample.RemoteAttachmentState.right()
    }

    private fun expectLoadingAttachmentStateLoadsParentUploadedStateSuccessful() {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns AttachmentStateSample.RemoteAttachmentState.copy(state = AttachmentSyncState.ExternalUploaded).right()
    }

    private fun expectLoadingAttachmentStateLoadsLocalStateSuccessful() {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns AttachmentStateSample.LocalAttachmentState.right()
    }

    private fun expectCancelAttachmentWorkerSucceeds() {
        coJustRun { attachmentRemoteDataSource.cancelAttachmentUpload(attachmentId) }
    }

    private fun expectLoadingAttachmentStateFailed() {
        coEvery {
            attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectDeleteAttachmentWithFileLocalSuccessful() {
        coEvery {
            attachmentLocalDataSource.deleteAttachmentWithFile(
                userId,
                messageId,
                attachmentId
            )
        } returns Unit.right()
    }

    private fun expectDeleteAttachmentLocalSuccessful() {
        coEvery {
            attachmentLocalDataSource.deleteAttachment(userId, messageId, attachmentId)
        } returns Unit.right()
    }

    private fun expectDeleteAttachmentLocalFailed() {
        coEvery {
            attachmentLocalDataSource.deleteAttachmentWithFile(userId, messageId, attachmentId)
        } returns DataError.Local.FailedToDeleteFile.left()
    }

    private fun expectUpsertAttachmentLocalFailed() {
        coEvery {
            attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, fileName, mimeType, byteContent)
        } returns DataError.Local.FailedToStoreFile.left()
    }

    private fun expectUpsertAttachmentLocalSuccessful() {
        coEvery {
            attachmentLocalDataSource.upsertAttachment(userId, messageId, attachmentId, fileName, mimeType, byteContent)
        } returns Unit.right()
    }

    private fun expectCreateOrUpdateAttachmentStateFailed(attachmentState: AttachmentState) {
        coEvery {
            attachmentStateLocalDataSource.createOrUpdate(attachmentState)
        } returns DataError.Local.Unknown.left()
    }

    private fun expectCreateOrUpdateAttachmentStateSuccessful(attachmentState: AttachmentState) {
        coEvery {
            attachmentStateLocalDataSource.createOrUpdate(attachmentState)
        } returns Unit.right()
    }
}

