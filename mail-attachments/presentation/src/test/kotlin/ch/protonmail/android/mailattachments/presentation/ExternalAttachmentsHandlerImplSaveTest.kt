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
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import arrow.core.left
import ch.protonmail.android.mailattachments.presentation.usecase.GenerateUniqueFileName
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ExternalAttachmentsHandlerImplSaveTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var attachmentsHandler: ExternalAttachmentsHandlerImpl

    private val generateUniqueFileName = mockk<GenerateUniqueFileName> {
        every { this@mockk.invoke(any()) } returns FileName
    }

    @BeforeTest
    fun setup() {
        val resolver = mockk<ContentResolver>(relaxed = true)
        contentResolver = resolver
        context = mockk { every { contentResolver } returns resolver }
        // ContentValues is a stub in the mockable android.jar; record the puts instead.
        mockkConstructor(ContentValues::class)
        every { anyConstructed<ContentValues>().put(any(), any<String>()) } just Runs
        attachmentsHandler = ExternalAttachmentsHandlerImpl(context, Dispatchers.Unconfined, generateUniqueFileName)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `saveFileToDownloadsFolder should emit an Error when the destination URI can't be determined`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()

        every { contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, any()) } returns null

        // When
        val actual = attachmentsHandler.saveFileToDownloadsFolder(sourceUri, FileName, MimeType)

        // Then
        verifyDownloadsEntry(FileName)
        assertEquals(ExternalAttachmentErrorResult.UnableToCreateUri.left(), actual)
        verify(exactly = 1) { contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, any()) }
        confirmVerified(contentResolver)
    }

    @Test
    fun `saveFileToDownloadsFolder should emit an Error when an error occurs during copy`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()
        val exception = IOException("Copy failed")

        val inputStream = object : InputStream() {
            override fun read(): Int {
                throw exception
            }
        }

        val outputStream = ByteArrayOutputStream()

        every {
            contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                any()
            )
        } returns destinationUri
        every { contentResolver.openInputStream(sourceUri) } returns inputStream
        every { contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        val actual = attachmentsHandler.saveFileToDownloadsFolder(sourceUri, FileName, MimeType)

        // Then
        verifyDownloadsEntry(FileName)
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
        verify(exactly = 1) { contentResolver.delete(destinationUri, null, null) }
    }

    @Test
    fun `saveFileToDownloadsFolder should emit an Error when the source file no longer exists`() = runTest {
        // Given
        val sourceUri = mockk<Uri>()
        val destinationUri = mockk<Uri>()

        every {
            contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                any()
            )
        } returns destinationUri
        every {
            contentResolver.openInputStream(sourceUri)
        } throws FileNotFoundException("open failed: ENOENT (No such file or directory)")

        // When
        val actual = attachmentsHandler.saveFileToDownloadsFolder(sourceUri, FileName, MimeType)

        // Then
        verifyDownloadsEntry(FileName)
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
        verify(exactly = 1) { contentResolver.delete(destinationUri, null, null) }
    }

    @Test
    fun `saveDataToDownloads should emit an Error when the destination URI can't be determined`() = runTest {
        // Given
        val fileName = "test_image.png"
        val data = byteArrayOf(1, 2, 3, 4)

        every { contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, any()) } returns null

        // When
        val actual = attachmentsHandler.saveDataToDownloads(fileName, MimeType, data)

        // Then
        verifyDownloadsEntry(fileName)
        assertEquals(ExternalAttachmentErrorResult.UnableToCreateUri.left(), actual)
        verify(exactly = 1) { contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, any()) }
        confirmVerified(contentResolver)
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

        every {
            contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                any()
            )
        } returns destinationUri
        every { contentResolver.openOutputStream(destinationUri) } returns outputStream

        // When
        val actual = attachmentsHandler.saveDataToDownloads(fileName, MimeType, data)

        // Then
        verifyDownloadsEntry(fileName)
        assertEquals(ExternalAttachmentErrorResult.UnableToCopy.left(), actual)
    }

    private fun verifyDownloadsEntry(displayName: String) {
        verify {
            anyConstructed<ContentValues>().put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            anyConstructed<ContentValues>().put(MediaStore.Downloads.MIME_TYPE, MimeType)
        }
    }

    private companion object {

        const val FileName = "fileName.pdf"
        const val MimeType = "image/png"
    }
}
