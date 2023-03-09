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

package ch.protonmail.android.mailcommon.data.file

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.assertNull

internal class FileHelperTest {

    private val fileStreamFactoryMock = mockk<FileHelper.FileStreamFactory>()
    private val fileFactoryMock = mockk<FileHelper.FileFactory>()
    private val testDispatcherProvider = TestDispatcherProvider()
    private val fileHelper = FileHelper(fileStreamFactoryMock, fileFactoryMock, testDispatcherProvider)

    @Test
    fun `should read contents of a file`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        val inputStream = ByteArrayInputStream(FileContent.toByteArray())
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.inputStreamFrom(file) } returns inputStream

        // When
        val fileContent = fileHelper.readFromFile(folder, filename)

        // Then
        assertEquals(FileContent, fileContent)
    }

    @Test
    fun `should return null when writing to a file failed`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.inputStreamFrom(file) } returns failingInputStream

        // When
        val fileContent = fileHelper.readFromFile(folder, filename)

        // Then
        assertNull(fileContent)
    }

    @Test
    fun `should write contents to a file`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        val outputStream = ByteArrayOutputStream()
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.outputStreamFrom(file) } returns outputStream

        // When
        val fileSaved = fileHelper.writeToFile(folder, filename, FileContent)

        // Then
        val actualContent = String(outputStream.toByteArray())
        assertEquals(FileContent, actualContent)
        assertTrue(fileSaved)
    }

    @Test
    fun `should return false when writing to a file failed`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.outputStreamFrom(file) } returns failingOutputStream

        // When
        val fileSaved = fileHelper.writeToFile(folder, filename, FileContent)

        // Then
        assertFalse(fileSaved)
    }

    @Test
    fun `should delete a file`() = runTest(testDispatcherProvider.Main) {
        // Given
        val fileMock = mockk<File> { every { delete() } returns true }
        every { fileFactoryMock.fileFrom(folder, filename) } returns fileMock

        // When
        val fileDeleted = fileHelper.deleteFile(folder, filename)

        // Then
        assertTrue(fileDeleted)
    }

    @Test
    fun `should return false when deleting file fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        val fileMock = mockk<File> { every { delete() } returns false }
        every { fileFactoryMock.fileFrom(folder, filename) } returns fileMock

        // When
        val fileDeleted = fileHelper.deleteFile(folder, filename)

        // Then
        assertFalse(fileDeleted)
    }

    @Test
    fun `should delete a folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::deleteRecursively)
        val fileMock = mockk<File> { every { deleteRecursively() } returns true }
        every { fileFactoryMock.folderFrom(folder) } returns fileMock

        // When
        val folderDeleted = fileHelper.deleteFolder(folder)

        // Then
        assertTrue(folderDeleted)
        unmockkStatic(File::deleteRecursively)
    }

    @Test
    fun `should return false when deleting folder fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::deleteRecursively)
        val fileMock = mockk<File> { every { deleteRecursively() } returns false }
        every { fileFactoryMock.folderFrom(folder) } returns fileMock

        // When
        val folderDeleted = fileHelper.deleteFolder(folder)

        // Then
        assertFalse(folderDeleted)
        unmockkStatic(File::deleteRecursively)
    }

    private companion object TestData {
        const val FileContent = "I am a file content"

        val folder = FileHelper.Folder("folder_path")
        val filename = FileHelper.Filename("file_name")
        val failingInputStream = object : InputStream() {
            override fun read(): Int = throw IllegalStateException()
        }
        val failingOutputStream = object : OutputStream() {
            override fun write(b: Int) = throw IllegalStateException()
        }
    }
}
