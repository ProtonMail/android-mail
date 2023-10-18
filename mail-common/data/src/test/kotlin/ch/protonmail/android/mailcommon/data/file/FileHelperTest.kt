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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.content.Context
import ch.protonmail.android.mailcommon.data.file.TestData.FileContent
import ch.protonmail.android.mailcommon.data.file.TestData.InternalStoragePath
import ch.protonmail.android.mailcommon.data.file.TestData.failingInputStream
import ch.protonmail.android.mailcommon.data.file.TestData.failingOutputStream
import ch.protonmail.android.mailcommon.data.file.TestData.filename
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertNull

internal open class FileHelperTest(folderPath: String) {

    private val contextMock = mockk<Context> {
        every { filesDir } returns File(InternalStoragePath)
    }
    protected val folder: FileHelper.Folder = FileHelper.Folder(folderPath)
    protected val fileStreamFactoryMock = mockk<FileHelper.FileStreamFactory>()
    protected val fileFactoryMock = mockk<FileHelper.FileFactory>()
    protected val testDispatcherProvider = TestDispatcherProvider()
    protected val fileHelper = FileHelper(fileStreamFactoryMock, fileFactoryMock, testDispatcherProvider, contextMock)
}

@RunWith(Parameterized::class)
internal class AllowedFoldersFileHelperTest(folderPath: String) : FileHelperTest(folderPath) {

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
    @Suppress("BlockingMethodInNonBlockingContext")
    fun `should read a file`() = runTest(testDispatcherProvider.Main) {
        // Given
        val expectedFile = File.createTempFile("test", "test")
        every { fileFactoryMock.fileFrom(folder, filename) } returns expectedFile

        // When
        val actual = fileHelper.getFile(folder, filename)

        // Then
        assertEquals(expectedFile, actual)
    }

    @Test
    fun `should return null when file doesn't exist`() = runTest(testDispatcherProvider.Main) {
        // Given
        val nonExistentFile = File(folder.path, filename.value)
        check(nonExistentFile.exists().not())
        every { fileFactoryMock.fileFrom(folder, filename) } returns nonExistentFile

        // When
        val result = fileHelper.getFile(folder, filename)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when reading a file failed`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.inputStreamFrom(file) } returns failingInputStream

        // When
        val result = fileHelper.readFromFile(folder, filename)

        // Then
        assertNull(result)
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
    fun `should return file when writing to a file as stream`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream(FileContent.toByteArray())
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.outputStreamFrom(file) } returns outputStream

        // When
        val fileSaved = fileHelper.writeToFileAsStream(folder, filename, inputStream)

        // Then
        assertTrue(fileSaved != null)
    }

    @Test
    fun `should return null when writing to a file as stream fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        val inputStream = ByteArrayInputStream(FileContent.toByteArray())
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.outputStreamFrom(file) } returns failingOutputStream

        // When
        val fileSaved = fileHelper.writeToFileAsStream(folder, filename, inputStream)

        // Then
        assertNull(fileSaved)
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

    @Test
    fun `should rename a folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::renameTo)
        val newFolder = FileHelper.Folder(folder.path + "_new")
        val newFileMock = mockk<File>()
        val oldFileMock = mockk<File> {
            every { renameTo(newFileMock) } returns true
        }
        every { fileFactoryMock.folderFromWhenExists(folder) } returns oldFileMock
        every { fileFactoryMock.folderFrom(newFolder) } returns newFileMock


        // When
        val folderRenamed = fileHelper.renameFolder(folder, newFolder)

        // Then
        assertTrue(folderRenamed)
        unmockkStatic(File::renameTo)
    }

    @Test
    fun `should return false when renaming a folder fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::renameTo)
        val newFolder = FileHelper.Folder(folder.path + "_new")
        val newFileMock = mockk<File>()
        val oldFileMock = mockk<File> { every { renameTo(newFileMock) } returns false }
        every { fileFactoryMock.folderFrom(folder) } returns oldFileMock
        every { fileFactoryMock.folderFrom(newFolder) } returns newFileMock


        // When
        val folderRenamed = fileHelper.renameFolder(folder, newFolder)

        // Then
        assertFalse(folderRenamed)
        unmockkStatic(File::renameTo)
    }

    @Test
    fun `should rename a file`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::renameTo)
        val newFilename = FileHelper.Filename(filename.value + "_new")
        val newFileMock = mockk<File>()
        val oldFileMock = mockk<File> { every { renameTo(newFileMock) } returns true }
        every { fileFactoryMock.fileFromWhenExists(folder, filename) } returns oldFileMock
        every { fileFactoryMock.fileFrom(folder, newFilename) } returns newFileMock

        // When
        val fileRenamed = fileHelper.renameFile(folder, filename, newFilename)

        // Then
        assertTrue(fileRenamed)
        unmockkStatic(File::renameTo)
    }

    @Test
    fun `should return false when renaming a file fails`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::renameTo)
        val newFilename = FileHelper.Filename(filename.value + "_new")
        val newFileMock = mockk<File>()
        val oldFileMock = mockk<File> { every { renameTo(newFileMock) } returns false }
        every { fileFactoryMock.fileFromWhenExists(folder, filename) } returns oldFileMock
        every { fileFactoryMock.fileFrom(folder, newFilename) } returns newFileMock

        // When
        val fileRenamed = fileHelper.renameFile(folder, filename, newFilename)

        // Then
        assertFalse(fileRenamed)
        unmockkStatic(File::renameTo)
    }

    @Test
    fun `should return false when renaming a file fails because file doesn't exist`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            mockkStatic(File::renameTo)
            val newFilename = FileHelper.Filename(filename.value + "_new")
            val newFileMock = mockk<File>()
            every { fileFactoryMock.fileFromWhenExists(folder, filename) } returns null
            every { fileFactoryMock.fileFrom(folder, newFilename) } returns newFileMock

            // When
            val fileRenamed = fileHelper.renameFile(folder, filename, newFilename)

            // Then
            assertFalse(fileRenamed)
            unmockkStatic(File::renameTo)
        }

    @Test
    fun `should return file when copying it from a source folder to a target folder was successful`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            mockkStatic(File::copyTo)
            val sourceFolder = FileHelper.Folder(folder.path + "_source")
            val sourceFilename = FileHelper.Filename(filename.value + "_source")
            val targetFolder = FileHelper.Folder(folder.path + "_target")
            val targetFilename = FileHelper.Filename(filename.value + "_target")
            val sourceFileMock = mockk<File>()
            val targetFileMock = mockk<File>()
            every { sourceFileMock.copyTo(targetFileMock) } returns targetFileMock
            every { fileFactoryMock.fileFrom(sourceFolder, sourceFilename) } returns sourceFileMock
            every { fileFactoryMock.fileFrom(targetFolder, targetFilename) } returns targetFileMock

            // When
            val copiedFile = fileHelper.copyFile(sourceFolder, sourceFilename, targetFolder, targetFilename)

            // Then
            assertEquals(targetFileMock, copiedFile)
            verify { sourceFileMock.copyTo(targetFileMock) }
            mockkStatic(File::copyTo)
        }

    @Test
    fun `should return null when copying file from a source folder to a target folder has failed`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            mockkStatic(File::copyTo)
            val sourceFolder = FileHelper.Folder(folder.path + "_source")
            val sourceFilename = FileHelper.Filename(filename.value + "_source")
            val targetFolder = FileHelper.Folder(folder.path + "_target")
            val targetFilename = FileHelper.Filename(filename.value + "_target")
            val sourceFileMock = mockk<File>()
            val targetFileMock = mockk<File>()
            every { sourceFileMock.copyTo(targetFileMock) } throws IOException()
            every { fileFactoryMock.fileFrom(sourceFolder, sourceFilename) } returns sourceFileMock
            every { fileFactoryMock.fileFrom(targetFolder, targetFilename) } returns targetFileMock

            // When
            val copiedFile = fileHelper.copyFile(sourceFolder, sourceFilename, targetFolder, targetFilename)

            // Then
            assertNull(copiedFile)
            verify { sourceFileMock.copyTo(targetFileMock) }
            mockkStatic(File::copyTo)
        }

    companion object {

        private val allowedFolders = listOf(
            "/data/user/0/ch.protonmail.android.alpha/files/",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/message_bodies/",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/attachments/",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/images/",
            "/storage/emulated/0",
            "/storage/emulated/1",
            "/storage/emulated/0/Download/",
            "/storage/emulated/0/Pictures/",
            "/storage/emulated/1/Movies/",
            "/storage/emulated/1/DCIM/",
            "/storage/emulated/1/../0/DCIM",
            "/data/user/0/ch.protonmail.android.alpha/databases/../../../../../storage/emulated/0",
            "/data/user/0/ch.protonmail.android.alpha/databases/../files/user1234/attachments"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = allowedFolders.map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
internal class BlacklistedFoldersFileHelperTest(folderPath: String) : FileHelperTest(folderPath) {

    @Test
    fun `should return null when trying to read from a blacklisted folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        val inputStream = ByteArrayInputStream(FileContent.toByteArray())
        every { fileFactoryMock.fileFrom(folder, filename) } returns file
        every { fileStreamFactoryMock.inputStreamFrom(file) } returns inputStream

        // When
        val fileContent = fileHelper.readFromFile(folder, filename)

        // Then
        assertNull(fileContent)
    }

    @Test
    fun `should return null when trying to get File from blacklisted folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        val file = File(folder.path, filename.value)
        every { fileFactoryMock.fileFrom(folder, filename) } returns file

        // When
        val fileContent = fileHelper.getFile(folder, filename)

        // Then
        assertNull(fileContent)
    }

    @Test
    fun `should return false and not write contents when trying to write to a blacklisted folder`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            val file = File(folder.path, filename.value)
            val outputStream = ByteArrayOutputStream()
            every { fileFactoryMock.fileFrom(folder, filename) } returns file
            every { fileStreamFactoryMock.outputStreamFrom(file) } returns outputStream

            // When
            val fileSaved = fileHelper.writeToFile(folder, filename, FileContent)

            // Then
            val actualContent = String(outputStream.toByteArray())
            assertEquals(EMPTY_STRING, actualContent)
            assertFalse(fileSaved)
        }

    @Test
    fun `should return null when trying to write a file to a blacklisted folder as stream`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            val file = File(folder.path, filename.value)
            val outputStream = ByteArrayOutputStream()
            val inputStream = ByteArrayInputStream(FileContent.toByteArray())
            every { fileFactoryMock.fileFrom(folder, filename) } returns file
            every { fileStreamFactoryMock.outputStreamFrom(file) } returns outputStream

            // When
            val fileSaved = fileHelper.writeToFileAsStream(folder, filename, inputStream)

            // Then
            assertNull(fileSaved)
        }

    @Test
    fun `should return false when trying to delete from a blacklisted folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        val fileMock = mockk<File>()
        every { fileFactoryMock.fileFrom(folder, filename) } returns fileMock

        // When
        val fileDeleted = fileHelper.deleteFile(folder, filename)

        // Then
        verify(exactly = 0) { fileMock.delete() }
        assertFalse(fileDeleted)
    }

    @Test
    fun `should return false when trying to delete a blacklisted folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::deleteRecursively)
        val fileMock = mockk<File>()
        every { fileFactoryMock.folderFrom(folder) } returns fileMock

        // When
        val folderDeleted = fileHelper.deleteFolder(folder)

        // Then
        assertFalse(folderDeleted)
        verify(exactly = 0) { fileMock.deleteRecursively() }
        unmockkStatic(File::deleteRecursively)
    }

    @Test
    fun `should return false when trying to rename a blacklisted folder`() = runTest(testDispatcherProvider.Main) {
        // Given
        mockkStatic(File::renameTo)
        val newFolder = FileHelper.Folder(folder.path + "_new")
        val newFileMock = mockk<File>()
        val fileMock = mockk<File> { every { renameTo(newFileMock) } returns true }
        every { fileFactoryMock.folderFrom(folder) } returns fileMock

        // When
        val folderRenamed = fileHelper.renameFolder(folder, newFolder)

        // Then
        assertFalse(folderRenamed)
        verify(exactly = 0) { fileMock.renameTo(newFileMock) }
        unmockkStatic(File::renameTo)
    }

    @Test
    fun `should return false when trying to rename a file in a blacklisted folder`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            mockkStatic(File::renameTo)
            val newFilename = FileHelper.Filename(filename.value + "_new")
            val newFileMock = mockk<File>()
            val fileMock = mockk<File> { every { renameTo(newFileMock) } returns true }

            // When
            val fileRenamed = fileHelper.renameFile(folder, filename, newFilename)

            // Then
            assertFalse(fileRenamed)
            verify(exactly = 0) { fileMock.renameTo(newFileMock) }
            unmockkStatic(File::renameTo)
        }

    @Test
    fun `should return null when trying to copy a file from a restricted folder`() =
        runTest(testDispatcherProvider.Main) {
            // Given
            mockkStatic(File::copyTo)
            val sourceFolder = FileHelper.Folder(folder.path + "_source")
            val sourceFilename = FileHelper.Filename(filename.value + "_source")
            val targetFolder = FileHelper.Folder(folder.path + "_target")
            val targetFilename = FileHelper.Filename(filename.value + "_target")
            val sourceFileMock = mockk<File>()
            val targetFileMock = mockk<File>()

            // When
            val copiedFile = fileHelper.copyFile(sourceFolder, sourceFilename, targetFolder, targetFilename)

            // Then
            assertNull(copiedFile)
            verify(exactly = 0) { sourceFileMock.copyTo(targetFileMock) }
            mockkStatic(File::copyTo)
        }

    companion object {

        private val blacklistedFolders = listOf(
            "/data/user/0/ch.protonmail.android.alpha/databases/",
            "/data/user/0/ch.protonmail.android.alpha/shared_prefs/",
            "/data/user/0/ch.protonmail.android.alpha/files/datastore/",
            "/data/user/0/ch.protonmail.android.alpha/files/../shared_prefs",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/images/../../../databases/",
            "/data/user/0/ch.protonmail.android.alpha/files/userid1234/message_bodies/../../../shared_prefs/",
            "/storage/emulated/0/../../../data/user/0/ch.protonmail.android.alpha/databases/"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = blacklistedFolders.map { arrayOf(it) }
    }
}

private object TestData {

    const val FileContent = "I am a file content"
    const val InternalStoragePath = "/data/user/0/ch.protonmail.android.alpha/files"

    val filename = FileHelper.Filename("file_name")
    val failingInputStream = object : InputStream() {
        override fun read(): Int = throw IllegalStateException()
    }
    val failingOutputStream = object : OutputStream() {
        override fun write(b: Int) = throw IllegalStateException()
    }
}
