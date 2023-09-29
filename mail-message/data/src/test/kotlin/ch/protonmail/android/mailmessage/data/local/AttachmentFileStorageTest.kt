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

import java.io.ByteArrayInputStream
import java.io.File
import android.net.Uri
import ch.protonmail.android.mailcommon.data.file.FileInformation
import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import ch.protonmail.android.mailcommon.data.file.UriHelper
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.AttachmentId
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.Content
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.File
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.FileInfo
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.MessageId
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorageTest.TestData.UserId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AttachmentFileStorageTest {

    private val uri = mockk<Uri>()
    private val inputStream = ByteArrayInputStream(Content)
    private val uriFileHelper = mockk<UriHelper>()
    private val internalFileStorageMock = mockk<InternalFileStorage>()
    private val attachmentFileStorage = AttachmentFileStorage(uriFileHelper, internalFileStorageMock)

    @Test
    fun `should save file in internal storage and return file when successful`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.writeFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                content = Content
            )
        } returns File

        // When
        val result = attachmentFileStorage.saveAttachment(UserId, MessageId, AttachmentId, Content)

        // Then
        assertEquals(File, result)
    }

    @Test
    fun `should save file provided by uri in internal storage and return file information when successful`() = runTest {
        // Given
        coEvery { uriFileHelper.readFromUri(uri) } returns inputStream
        coEvery { uriFileHelper.getFileInformationFromUri(uri) } returns FileInfo
        coEvery {
            internalFileStorageMock.writeFileAsStream(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                inputStream = inputStream
            )
        } returns File

        // When
        val result = attachmentFileStorage.saveAttachment(UserId, MessageId, AttachmentId, uri)

        // Then
        assertEquals(FileInfo, result)
    }

    @Test
    fun `should save file in cache storage and return file when successful`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.writeCachedFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                content = Content
            )
        } returns File

        // When
        val result = attachmentFileStorage.saveAttachmentCached(UserId, MessageId, AttachmentId, Content)

        // Then
        assertEquals(File, result)
    }

    @Test
    fun `should return null when saving file in internal storage fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.writeFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                content = Content
            )
        } returns null

        // When
        val result = attachmentFileStorage.saveAttachment(UserId, MessageId, AttachmentId, Content)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when saving file provided by uri in internal storage fails`() = runTest {
        // Given
        coEvery { uriFileHelper.readFromUri(uri) } returns inputStream
        coEvery {
            internalFileStorageMock.writeFileAsStream(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                inputStream = inputStream
            )
        } returns null

        // When
        val result = attachmentFileStorage.saveAttachment(UserId, MessageId, AttachmentId, uri)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when saving file in cache storage fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.writeCachedFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                content = Content
            )
        } returns null

        // When
        val result = attachmentFileStorage.saveAttachmentCached(UserId, MessageId, AttachmentId, Content)

        // Then
        assertNull(result)
    }

    @Test
    fun `should update folder in internal storage`() = runTest {
        // Given
        val updatedMessageId = MessageId + "_new"
        coJustRun {
            internalFileStorageMock.renameFolder(
                userId = UserId,
                oldFolder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                newFolder = InternalFileStorage.Folder.MessageAttachments(updatedMessageId)
            )
        }

        // When
        attachmentFileStorage.updateParentFolderForAttachments(UserId, MessageId, updatedMessageId)

        // Then
        coVerify {
            internalFileStorageMock.renameFolder(
                userId = UserId,
                oldFolder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                newFolder = InternalFileStorage.Folder.MessageAttachments(updatedMessageId)
            )
        }
    }

    @Test
    fun `should update file in internal storage`() = runTest {
        // Given
        val updatedAttachmentId = AttachmentId + "_new"
        coJustRun {
            internalFileStorageMock.renameFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                oldFileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                newFileIdentifier = InternalFileStorage.FileIdentifier(updatedAttachmentId)
            )
        }

        // When
        attachmentFileStorage.updateFileNameForAttachment(UserId, MessageId, AttachmentId, updatedAttachmentId)

        // Then
        coVerify {
            internalFileStorageMock.renameFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                oldFileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId),
                newFileIdentifier = InternalFileStorage.FileIdentifier(updatedAttachmentId)
            )
        }
    }

    @Test
    fun `should read file from internal storage and return file when successful`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.getFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId)
            )
        } returns File

        // When
        val result = attachmentFileStorage.readAttachment(UserId, MessageId, AttachmentId)

        // Then
        assertEquals(File, result)
    }

    @Test
    fun `should read file from cache storage and return file when successful`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.getCachedFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId)
            )
        } returns File

        // When
        val result = attachmentFileStorage.readCachedAttachment(UserId, MessageId, AttachmentId)

        // Then
        assertEquals(File, result)
    }

    @Test
    fun `should throw exception when reading file from internal storage fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.getFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId)
            )
        } returns null

        // Then
        assertFailsWith<AttachmentFileReadException> {
            // When
            attachmentFileStorage.readAttachment(UserId, MessageId, AttachmentId)
        }
    }

    @Test
    fun `should throw exception when reading file from cache storage fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.getCachedFile(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId),
                fileIdentifier = InternalFileStorage.FileIdentifier(AttachmentId)
            )
        } returns null

        // Then
        assertFailsWith<AttachmentFileReadException> {
            // When
            attachmentFileStorage.readCachedAttachment(UserId, MessageId, AttachmentId)
        }
    }

    @Test
    fun `should delete all files connected to a message from internal storage and return true on success`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFolder(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId)
            )
        } returns true

        // When
        val result = attachmentFileStorage.deleteAttachmentsOfMessage(UserId, MessageId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `should delete all cached files connected to a message from internal storage and return true on success`() =
        runTest {
            // Given
            coEvery {
                internalFileStorageMock.deleteCachedFolder(
                    userId = UserId,
                    folder = InternalFileStorage.Folder.MessageAttachments(MessageId)
                )
            } returns true

            // When
            val result = attachmentFileStorage.deleteCachedAttachmentsOfMessage(UserId, MessageId)

            // Then
            assertTrue(result)
        }

    @Test
    fun `should return false when deleting all files connected to a message from internal fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFolder(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId)
            )
        } returns false

        // When
        val result = attachmentFileStorage.deleteAttachmentsOfMessage(UserId, MessageId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return false when deleting all cached files connected to a message from internal fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteCachedFolder(
                userId = UserId,
                folder = InternalFileStorage.Folder.MessageAttachments(MessageId)
            )
        } returns false

        // When
        val result = attachmentFileStorage.deleteCachedAttachmentsOfMessage(UserId, MessageId)

        // Then
        assertFalse(result)
    }

    object TestData {

        val File = File("")
        val FileInfo = FileInformation("name", 123, "mimeType")
        val UserId = UserIdSample.Primary
        val MessageId = MessageIdSample.Invoice.id
        val Content = "content".toByteArray()
        const val AttachmentId = "attachmentId"
    }
}
