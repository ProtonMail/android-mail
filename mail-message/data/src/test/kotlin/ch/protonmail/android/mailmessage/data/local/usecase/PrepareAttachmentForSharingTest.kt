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

package ch.protonmail.android.mailmessage.data.local.usecase

import java.io.OutputStream
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcommon.domain.system.ContentValuesProvider
import ch.protonmail.android.mailmessage.data.local.provider.GetUriFromMediaScanner
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class PrepareAttachmentForSharingTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentId = AttachmentId("invoice")
    private val decryptedByteArray = "I'm a decrypted file content".toByteArray()
    private val messageWithBody = MessageWithBody(
        MessageTestData.message,
        MessageBodyTestData.messageBodyWithAttachment
    )
    private val environmentFile by lazy { temporaryFolder.newFolder() }
    private val mockedContentResolver = mockk<ContentResolver>()
    private val context = mockk<Context> {
        every { contentResolver } returns mockedContentResolver
    }


    private val messageRepository = mockk<MessageRepository> {
        coEvery { getLocalMessageWithBody(userId, messageId) } returns messageWithBody
    }
    private val buildVersionProvider = mockk<BuildVersionProvider>()
    private val contentValuesProvider = mockk<ContentValuesProvider>()
    private val getUriFromMediaScanner = mockk<GetUriFromMediaScanner>()
    private val sanitizeFullFileName = spyk<SanitizeFullFileName>()
    private val generateUniqueFileName = spyk<GenerateUniqueFileName>()
    private val prepareAttachmentForSharing =
        PrepareAttachmentForSharing(
            context,
            messageRepository,
            buildVersionProvider,
            contentValuesProvider,
            getUriFromMediaScanner,
            sanitizeFullFileName,
            generateUniqueFileName,
            UnconfinedTestDispatcher()
        )

    @Before
    fun setup() {
        mockkStatic(Environment::class)
        every { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) } returns environmentFile
    }

    @After
    fun tearDown() {
        unmockkStatic(Environment::class)
    }

    @Test
    fun `return message not found error when message body is not found`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns null

        // When
        val result = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray)

        // Then
        assertEquals(PrepareAttachmentForSharingError.MessageNotFound.left(), result)
    }

    @Test
    fun `return attachment not found when attachment is not found in message body`() = runTest {
        // Given
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns messageWithBody.copy(
            messageBody = MessageBodyTestData.messageBody
        )

        // When
        val result = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray)

        // Then
        assertEquals(PrepareAttachmentForSharingError.AttachmentNotFound.left(), result)
    }

    @Test
    fun `return uri when storing attachment was successful and current api version is after Q`() = runTest {
        // Given
        val expectedUri = mockk<Uri>()
        val expectedAttachment = MessageAttachmentSample.invoice
        val outputStream = mockk<OutputStream>(relaxed = true)

        val contentValues = mockk<ContentValues>(relaxUnitFun = true) {
            every { put(MediaStore.Downloads.DISPLAY_NAME, expectedAttachment.name) } just Runs
            every { put(MediaStore.Downloads.MIME_TYPE, expectedAttachment.mimeType) } just Runs
            every { put(MediaStore.Downloads.IS_PENDING, 1) } just Runs
            every { put(MediaStore.Downloads.IS_PENDING, 0) } just Runs
        }
        provideSdkAfterQ()
        coEvery { contentValuesProvider.provideContentValues() } returns contentValues
        coEvery {
            mockedContentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } returns expectedUri
        coEvery { mockedContentResolver.openOutputStream(expectedUri) } returns outputStream
        coEvery { mockedContentResolver.update(expectedUri, contentValues, null, null) } returns 1

        // When
        val result = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray)

        // Then
        assertEquals(expectedUri.right(), result)
        verify { sanitizeFullFileName(expectedAttachment.name) }
    }

    @Test
    fun `throw exception when storing attachment failed and current api version is after Q`() = runTest {
        // Given
        val expectedAttachment = MessageAttachmentSample.invoice

        val contentValues = mockk<ContentValues>(relaxUnitFun = true) {
            every { put(MediaStore.Downloads.DISPLAY_NAME, expectedAttachment.name) } just Runs
            every { put(MediaStore.Downloads.MIME_TYPE, expectedAttachment.mimeType) } just Runs
            every { put(MediaStore.Downloads.IS_PENDING, 1) } just Runs
            every { put(MediaStore.Downloads.IS_PENDING, 0) } just Runs
        }
        provideSdkAfterQ()
        coEvery { contentValuesProvider.provideContentValues() } returns contentValues
        coEvery {
            mockedContentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } returns null

        // When
        val result = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray)

        // - Then
        assertEquals(PrepareAttachmentForSharingError.PreparingAttachmentFailed.left(), result)
    }


    @Test
    fun `return uri when current api version is before Q`() = runTest {
        // Given
        provideSdkBeforeQ()
        val expectedUri = mockk<Uri>()
        val expectedAttachment = MessageAttachmentSample.invoice
        coEvery { getUriFromMediaScanner(any(), expectedAttachment.mimeType) } returns expectedUri

        // When
        val result = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray)

        // Then
        assertEquals(expectedUri.right(), result)
        verify { sanitizeFullFileName(expectedAttachment.name) }
    }


    private fun provideSdkBeforeQ() {
        every { buildVersionProvider.sdkInt() } returns Build.VERSION_CODES.P
    }

    private fun provideSdkAfterQ() {
        every { buildVersionProvider.sdkInt() } returns Build.VERSION_CODES.Q
    }
}
