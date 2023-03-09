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
import android.content.Context
import ch.protonmail.android.mailcommon.data.file.FileHelper
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.testdata.message.MessageBodyTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

internal class MessageBodyFileStorageTest {

    private val contextMock = mockk<Context> {
        every { filesDir } returns File(InternalStoragePath)
    }
    private val fileHelperMock = mockk<FileHelper>()
    private val messageBodyFileStorage = MessageBodyFileStorage(contextMock, fileHelperMock)

    @Test
    fun `should read message body using sanitised folder and filename`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.readFromFile(messageBodyFolder, filename) } returns MessageBody

        // When
        val actualMessageBody = messageBodyFileStorage.readMessageBody(
            UserId(UserIdString),
            MessageId(MessageIdString)
        )

        // Then
        assertEquals(MessageBody, actualMessageBody)
    }

    @Test
    fun `should return null when reading body using sanitised folder and filename fails`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.readFromFile(messageBodyFolder, filename) } returns null

        // When
        val actualMessageBody = messageBodyFileStorage.readMessageBody(
            UserId(UserIdString),
            MessageId(MessageIdString)
        )

        // Then
        assertNull(actualMessageBody)
    }

    @Test
    fun `should save message body using sanitised folder and filename and return true on success`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        val savedBody = MessageBodyTestData.buildMessageBody(messageId = MessageId(MessageIdString), body = MessageBody)
        coEvery { fileHelperMock.writeToFile(messageBodyFolder, filename, MessageBody) } returns true

        // When
        val messageSaved = messageBodyFileStorage.saveMessageBody(UserId(UserIdString), savedBody)

        // Then
        assertTrue(messageSaved)
    }

    @Test
    fun `should save message body using a sanitised folder and filename and return false on failure`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        val savedBody = MessageBodyTestData.buildMessageBody(messageId = MessageId(MessageIdString), body = MessageBody)
        coEvery { fileHelperMock.writeToFile(messageBodyFolder, filename, MessageBody) } returns false

        // When
        val messageSaved = messageBodyFileStorage.saveMessageBody(UserId(UserIdString), savedBody)

        // Then
        assertFalse(messageSaved)
    }

    @Test
    fun `should delete message using sanitised folder and filename and return true on success`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.deleteFile(messageBodyFolder, filename) } returns true

        // When
        val messageDeleted = messageBodyFileStorage.deleteMessageBody(UserId(UserIdString), MessageId(MessageIdString))

        // Then
        assertTrue(messageDeleted)
    }

    @Test
    fun `should delete message using sanitised folder and filename and return false on failure`() = runTest {
        // Given
        val filename = FileHelper.Filename(SanitisedMessageIdString)
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.deleteFile(messageBodyFolder, filename) } returns false

        // When
        val messageDeleted = messageBodyFileStorage.deleteMessageBody(UserId(UserIdString), MessageId(MessageIdString))

        // Then
        assertFalse(messageDeleted)
    }

    @Test
    fun `should delete all message bodies using sanitised folder and return true on success`() = runTest {
        // Given
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.deleteFolder(messageBodyFolder) } returns true

        // When
        val messagesDeleted = messageBodyFileStorage.deleteAllMessageBodies(UserId(UserIdString))

        // Then
        assertTrue(messagesDeleted)
    }

    @Test
    fun `should delete all message bodies using sanitised folder and return false on failure`() = runTest {
        // Given
        val messageBodyFolder = FileHelper.Folder("$MessageBodyFolderBase$SanitisedUserIdString/")
        coEvery { fileHelperMock.deleteFolder(messageBodyFolder) } returns false

        // When
        val messagesDeleted = messageBodyFileStorage.deleteAllMessageBodies(UserId(UserIdString))

        // Then
        assertFalse(messagesDeleted)
    }

    private companion object TestData {

        const val MessageIdString = "123 32/1"
        const val SanitisedMessageIdString = "123_32:1"
        const val UserIdString = "456 78/9"
        const val SanitisedUserIdString = "456_78:9"
        const val InternalStoragePath = "/some/path/to/internal/storage"
        const val MessageBody = "I am a message body"
        const val MessageBodyFolderBase = "$InternalStoragePath/message_bodies/"
    }
}
