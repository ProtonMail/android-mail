/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import arrow.core.left
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.usecase.GenerateUniqueFileName
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class ExternalAttachmentsHandlerImplSaveTest {

    private lateinit var context: Context
    private lateinit var attachmentsHandler: ExternalAttachmentsHandlerImpl

    private val generateUniqueFileName = mockk<GenerateUniqueFileName> {
        every { this@mockk.invoke(any()) } returns FileName
    }

    @BeforeTest
    fun setup() {
        context = spyk(RuntimeEnvironment.getApplication().applicationContext)
        attachmentsHandler = ExternalAttachmentsHandlerImpl(context, Dispatchers.Unconfined, generateUniqueFileName)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `saveFileToDownloadsFolder should emit an Error when the destination URI can't be determined`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val fileContent = FileContent(FileName, sourceUri, MimeType)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FileName)
            put(MediaStore.Downloads.MIME_TYPE, fileContent.mimeType)
        }

        every { context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) } returns null

        // When
        val actual = attachmentsHandler.saveFileToDownloadsFolder(fileContent)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCreateUri.left(), actual)
        verify(exactly = 1) { context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) }
        confirmVerified(context.contentResolver)
    }

    @Test
    fun `saveFileToDownloadsFolder should emit an Error when an error occurs during copy`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        val exception = IOException("Copy failed")
        val attachmentInput = FileContent(FileName, sourceUri, MimeType)

        val inputStream = object : InputStream() {
            override fun read(): Int {
                throw exception
            }
        }

        val outputStream = ByteArrayOutputStream()

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FileName)
            put(MediaStore.Downloads.MIME_TYPE, attachmentInput.mimeType)
        }

        every {
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
        } returns destinationUri
        every { context.contentResolver.openInputStream(sourceUri) } returns inputStream
        every { context.contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        val actual = attachmentsHandler.saveFileToDownloadsFolder(attachmentInput)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
    }

    @Test
    fun `saveDataToDownloads should emit an Error when the destination URI can't be determined`() = runTest {
        // Given
        val fileName = "test_image.png"
        val data = byteArrayOf(1, 2, 3, 4)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MimeType)
        }

        every { context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) } returns null

        // When
        val actual = attachmentsHandler.saveDataToDownloads(fileName, MimeType, data)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCreateUri.left(), actual)
        verify(exactly = 1) { context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) }
        confirmVerified(context.contentResolver)
    }

    @Test
    fun `saveDataToDownloads should emit an Error when an error occurs during write`() = runTest {
        // Given
        val fileName = "test_image.png"
        val data = byteArrayOf(1, 2, 3, 4)
        val destinationUri = mockk<Uri>()
        val exception = IOException("Write failed")

        val outputStream = object : ByteArrayOutputStream() {
            override fun write(b: ByteArray) {
                throw exception
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MimeType)
        }

        every {
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
        } returns destinationUri
        every { context.contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        val actual = attachmentsHandler.saveDataToDownloads(fileName, MimeType, data)

        // Then
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
    }

    private companion object {

        const val FileName = "fileName.pdf"
        const val MimeType = "image/png"
    }
}
