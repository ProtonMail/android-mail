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

import java.io.File
import android.content.Context
import android.util.Base64
import arrow.core.left
import ch.protonmail.android.mailcommon.data.file.FileHelper
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PastedImageRepositoryImplTest {

    private val cacheDirectory = File("/cache")
    private val context = mockk<Context> {
        every { cacheDir } returns cacheDirectory
    }
    private val fileHelper = mockk<FileHelper>()

    private val repository = PastedImageRepositoryImpl(
        context,
        Dispatchers.Unconfined,
        CoroutineScope(Dispatchers.Unconfined),
        fileHelper
    )

    private val expectedFolder = FileHelper.Folder(File(cacheDirectory, PastedImagesDirName).absolutePath)

    @BeforeTest
    fun setUp() {
        mockkStatic(Base64::class)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given jpeg magic bytes, when saving base64 image, then stores it with a jpg extension`() = runTest {
        // Given
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".jpg"))
    }

    @Test
    fun `given gif magic bytes, when saving base64 image, then stores it with a gif extension`() = runTest {
        // Given
        val bytes = byteArrayOf(0x47, 0x49, 0x46, 0x38)

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".gif"))
    }

    @Test
    fun `given webp riff container, when saving base64 image, then stores it with a webp extension`() = runTest {
        // Given
        val bytes = byteArrayOf(
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            0x00, 0x00, 0x00, 0x00, // chunk size
            0x57, 0x45, 0x42, 0x50 // "WEBP"
        )

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".webp"))
    }

    @Test
    fun `given bmp magic bytes, when saving base64 image, then stores it with a bmp extension`() = runTest {
        // Given
        val bytes = byteArrayOf(0x42, 0x4D, 0x00, 0x00) // "BM"

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".bmp"))
    }

    @Test
    fun `given heic ftyp container, when saving base64 image, then stores it with a heic extension`() = runTest {
        // Given
        val bytes = byteArrayOf(
            0x00, 0x00, 0x00, 0x18, // box size
            0x66, 0x74, 0x79, 0x70, // "ftyp"
            0x68, 0x65, 0x69, 0x63 // "heic" brand
        )

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".heic"))
    }

    @Test
    fun `given unrecognised bytes, when saving base64 image, then defaults to a png extension`() = runTest {
        // Given
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)

        // When
        val filename = captureStoredFilename(bytes)

        // Then
        assertTrue(filename.endsWith(".png"))
    }

    @Test
    fun `given writing to cache fails, when saving base64 image, then returns failed to store file error`() = runTest {
        // Given
        every { Base64.decode(EncodedImage, any()) } returns byteArrayOf(0x00)
        coEvery { fileHelper.writeToFile(any(), any(), any<ByteArray>()) } returns null

        // When
        val result = repository.saveBase64ImageToCache(EncodedImage)

        // Then
        assertEquals(DataError.Local.FailedToStoreFile.left(), result)
    }

    @Test
    fun `given a staging area, when clearing it, then deletes the pasted images cache folder`() {
        // Given
        coEvery { fileHelper.deleteFolder(expectedFolder) } returns true

        // When
        repository.clearStagingArea()

        // Then
        coVerify(exactly = 1) { fileHelper.deleteFolder(expectedFolder) }
    }

    private suspend fun captureStoredFilename(bytes: ByteArray): String {
        every { Base64.decode(EncodedImage, any()) } returns bytes
        val filenameSlot = slot<FileHelper.Filename>()
        coEvery {
            fileHelper.writeToFile(expectedFolder, capture(filenameSlot), bytes)
        } returns File(cacheDirectory, "$PastedImagesDirName/out")
        repository.saveBase64ImageToCache(EncodedImage)
        return filenameSlot.captured.value
    }

    private companion object {

        const val EncodedImage = "encoded-image"
        const val PastedImagesDirName = "pasted_image_staging"
    }
}
