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
import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AttachmentFileStorageTest {

    private val internalFileStorageMock = mockk<InternalFileStorage>()
    private val attachmentFileStorage = AttachmentFileStorage(internalFileStorageMock)

    @Test
    fun `should save file in internal storage and return file when successful`() = runTest {
        // Given
        val file = File("")
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        val attachmentId = "attachmentId"
        val content = "content".toByteArray()
        coEvery {
            internalFileStorageMock.writeFile(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(attachmentId),
                content = content
            )
        } returns file

        // When
        val result = attachmentFileStorage.saveAttachment(userId, messageId, attachmentId, content)

        // Then
        assertEquals(file, result)
    }

    @Test
    fun `should return null when saving file in internal storage fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        val attachmentId = "attachmentId"
        val content = "content".toByteArray()
        coEvery {
            internalFileStorageMock.writeFile(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(attachmentId),
                content = content
            )
        } returns null

        // When
        val result = attachmentFileStorage.saveAttachment(userId, messageId, attachmentId, content)

        // Then
        assertNull(result)
    }

    @Test
    fun `should read file from internal storage and return file when successful`() = runTest {
        // Given
        val file = File("")
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        val attachmentId = "attachmentId"
        coEvery {
            internalFileStorageMock.getFile(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(attachmentId)
            )
        } returns file

        // When
        val result = attachmentFileStorage.readAttachment(userId, messageId, attachmentId)

        // Then
        assertEquals(file, result)
    }

    @Test
    fun `should throw exception when reading file from internal storage fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        val attachmentId = "attachmentId"
        coEvery {
            internalFileStorageMock.getFile(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(attachmentId)
            )
        } returns null

        // Then
        assertFailsWith<AttachmentFileReadException> {
            // When
            attachmentFileStorage.readAttachment(userId, messageId, attachmentId)
        }
    }

    @Test
    fun `should delete all files connected to a message from internal storage and return true on success`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        coEvery {
            internalFileStorageMock.deleteFolder(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId)
            )
        } returns true

        // When
        val result = attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should delete all files connected to a message from internal storage and return false on failure`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice.id
        coEvery {
            internalFileStorageMock.deleteFolder(
                userId = userId,
                folder = InternalFileStorage.Folder.MessageAttachments(messageId)
            )
        } returns false

        // When
        val result = attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId)

        // Then
        assertFalse(result)
    }
}
