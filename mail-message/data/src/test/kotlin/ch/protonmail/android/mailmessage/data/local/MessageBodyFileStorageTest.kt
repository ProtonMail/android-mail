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

import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MessageBodyFileStorageTest {

    private val internalFileStorageMock = mockk<InternalFileStorage>()
    private val messageBodyFileStorage = MessageBodyFileStorage(internalFileStorageMock)

    @Test
    fun `should read message body from the internal file storage`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.readFromFile(
                UserIdSample.Primary,
                InternalFileStorage.Folder.MessageBodies,
                InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id)
            )
        } returns MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY

        // When
        val actualMessageBody = messageBodyFileStorage.readMessageBody(
            UserId(UserIdSample.Primary.id),
            MessageIdSample.Invoice
        )

        // Then
        assertEquals(MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY, actualMessageBody)
    }

    @Test(expected = MessageBodyFileReadException::class)
    fun `should throw an exception when reading body from internal storage fails`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.readFromFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id)
            )
        } returns null

        // When
        messageBodyFileStorage.readMessageBody(
            UserIdSample.Primary,
            MessageIdSample.Invoice
        )

        // Then
        // Exception is thrown
    }

    @Test
    fun `should save message body in internal storage`() = runTest {
        // Given
        val savedBody = MessageBodyTestData.buildMessageBody(
            messageId = MessageIdSample.Invoice,
            body = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        )
        coEvery {
            internalFileStorageMock.writeToFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id),
                content = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
            )
        } returns true

        // When
        messageBodyFileStorage.saveMessageBody(UserIdSample.Primary, savedBody)

        // Then
        coVerify {
            internalFileStorageMock.writeToFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id),
                content = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
            )
        }
    }

    @Test(expected = MessageBodyFileWriteException::class)
    fun `should throw an exception when saving message body in internal storage fails`() = runTest {
        // Given
        val savedBody = MessageBodyTestData.buildMessageBody(
            messageId = MessageIdSample.Invoice,
            body = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        )
        coEvery {
            internalFileStorageMock.writeToFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id),
                content = MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
            )
        } returns false

        // When
        messageBodyFileStorage.saveMessageBody(UserIdSample.Primary, savedBody)

        // Then
        // Exception is thrown
    }

    @Test
    fun `should delete message from internal storage and return true on success`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id)
            )
        } returns true


        // When
        val messageDeleted = messageBodyFileStorage.deleteMessageBody(UserIdSample.Primary, MessageIdSample.Invoice)

        // Then
        assertTrue(messageDeleted)
    }

    @Test
    fun `should delete message from internal storage and return false on failure`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFile(
                userId = UserIdSample.Primary,
                folder = InternalFileStorage.Folder.MessageBodies,
                fileIdentifier = InternalFileStorage.FileIdentifier(MessageIdSample.Invoice.id)
            )
        } returns false


        // When
        val messageDeleted = messageBodyFileStorage.deleteMessageBody(UserIdSample.Primary, MessageIdSample.Invoice)

        // Then
        assertFalse(messageDeleted)
    }

    @Test
    fun `should delete all message bodies from internal storage and return true on success`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFolder(UserIdSample.Primary, InternalFileStorage.Folder.MessageBodies)
        } returns true


        // When
        val messagesDeleted = messageBodyFileStorage.deleteAllMessageBodies(UserIdSample.Primary)

        // Then
        assertTrue(messagesDeleted)
    }

    @Test
    fun `should delete all message bodies from internal storage and return false on failure`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.deleteFolder(UserIdSample.Primary, InternalFileStorage.Folder.MessageBodies)
        } returns false


        // When
        val messagesDeleted = messageBodyFileStorage.deleteAllMessageBodies(UserIdSample.Primary)

        // Then
        assertFalse(messagesDeleted)
    }

    @Test
    fun `should rename file for message body`() = runTest {
        // Given
        coEvery {
            internalFileStorageMock.renameFile(
                UserIdSample.Primary,
                InternalFileStorage.Folder.MessageBodies,
                InternalFileStorage.FileIdentifier(MessageIdSample.LocalDraft.id),
                InternalFileStorage.FileIdentifier(MessageIdSample.RemoteDraft.id)
            )
        } returns true

        // When
        messageBodyFileStorage.updateFileNameForMessageBody(
            UserIdSample.Primary, MessageIdSample.LocalDraft, MessageIdSample.RemoteDraft
        )

        // Then
        coVerify {
            internalFileStorageMock.renameFile(
                UserIdSample.Primary,
                InternalFileStorage.Folder.MessageBodies,
                InternalFileStorage.FileIdentifier(MessageIdSample.LocalDraft.id),
                InternalFileStorage.FileIdentifier(MessageIdSample.RemoteDraft.id)
            )
        }
    }
}
