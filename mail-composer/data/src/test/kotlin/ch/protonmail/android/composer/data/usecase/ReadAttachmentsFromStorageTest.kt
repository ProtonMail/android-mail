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
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.ApiMessageId
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.Attachment1
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.Attachment2
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.AttachmentId1
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.AttachmentId2
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.AttachmentIds
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.DraftState
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.MessageId
import ch.protonmail.android.composer.data.usecase.ReadAttachmentsFromStorageTest.TestData.UserId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadAttachmentsFromStorageTest {

    private val attachmentRepository = mockk<AttachmentRepository> {
        coEvery { readFileFromStorage(UserId, ApiMessageId, AttachmentId1) } returns Attachment1.right()
        coEvery { readFileFromStorage(UserId, ApiMessageId, AttachmentId2) } returns Attachment2.right()
    }
    private val draftStateRepository = mockk<DraftStateRepository> {
        coEvery { observe(UserId, MessageId) } returns flowOf(DraftState.right())
    }

    private val readAttachmentsFromStorage = ReadAttachmentsFromStorage(attachmentRepository, draftStateRepository)

    @Test
    fun `should return files if all attachments were read successfully`() = runTest {
        // Given
        val expected = mapOf(AttachmentId1 to Attachment1, AttachmentId2 to Attachment2).right()

        // When
        val actual = readAttachmentsFromStorage(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return error if at least one attachment was not read successfully`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        coEvery {
            attachmentRepository.readFileFromStorage(UserId, ApiMessageId, AttachmentId2)
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = readAttachmentsFromStorage(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return error if api assigned message id still doesn't exist`() = runTest {
        // Given
        val expected = DataError.Local.NoDataCached.left()
        coEvery { draftStateRepository.observe(UserId, MessageId) } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val actual = readAttachmentsFromStorage(UserId, MessageId, AttachmentIds)

        // Then
        assertEquals(expected, actual)
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

        val DraftState = DraftState(UserId, MessageId, ApiMessageId, DraftSyncState.Synchronized, DraftAction.Compose)
    }
}
