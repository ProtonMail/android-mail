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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.mailmessage.data.repository.AttachmentRepositoryImpl
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AttachmentRepositoryImplTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("attachmentId")
    private val attachmentMetaData = MessageAttachmentMetadata(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        hash = "hash",
        path = "path",
        status = AttachmentWorkerStatus.Success
    )


    private val localDataSource: AttachmentLocalDataSource = mockk()
    private val remoteDataSource: AttachmentRemoteDataSource = mockk()

    private val repository = AttachmentRepositoryImpl(remoteDataSource, localDataSource)

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

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(attachmentMetaData.right(), result)
    }

    @Test
    fun `should return null when remote call fails`() = runTest {
        // Given
        coEvery {
            localDataSource.getAttachment(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
        coEvery { remoteDataSource.getAttachment(userId, messageId, attachmentId) } returns Unit
        coEvery { localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId) } returns flowOf(
            attachmentMetaData.copy(
                hash = null,
                path = null,
                status = AttachmentWorkerStatus.Failed
            )
        )

        // When
        val result = repository.getAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(DataError.Remote.Unknown.left(), result)
    }

    @Test
    fun `should return local stored attachment metadata when found by hash`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        coEvery { localDataSource.getAttachmentMetadataByHash(attachmentHash) } returns attachmentMetaData.right()

        // When
        val result = repository.getAttachmentMetadataByHash(attachmentHash)

        // Then
        assertEquals(attachmentMetaData.right(), result)
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `should return no data cached when no message metadata was found by hash`() = runTest {
        // Given
        val attachmentHash = "attachmentHash"
        coEvery {
            localDataSource.getAttachmentMetadataByHash(attachmentHash)
        } returns DataError.Local.NoDataCached.left()

        // When
        val result = repository.getAttachmentMetadataByHash(attachmentHash)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
        coVerify { remoteDataSource wasNot Called }
    }

}
