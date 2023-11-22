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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class FileHelper @Inject constructor(
    private val fileStreamFactory: FileStreamFactory,
    private val fileFactory: FileFactory,
    private val dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val applicationContext: Context
) {

    suspend fun readFromFile(folder: Folder, filename: Filename): String? = fileOperationIn(folder) {
        val fileToRead = fileFactory.fileFrom(folder, filename)
        runCatching {
            fileStreamFactory.inputStreamFrom(fileToRead)
                .bufferedReader()
                .use { it.readText() }
        }.getOrNull()
    }

    suspend fun getFile(folder: Folder, filename: Filename): File? = fileOperationIn(folder) {
        runCatching { fileFactory.fileFrom(folder, filename).takeIf { it.exists() } }.getOrNull()
    }

    suspend fun getFolder(folderName: Folder): File? = fileOperationIn(folderName) {
        runCatching { fileFactory.folderFrom(folderName) }.getOrNull()
    }

    suspend fun renameFolder(oldFolder: Folder, newFolder: Folder) = fileOperationIn(newFolder) {
        runCatching {
            fileFactory.folderFromWhenExists(oldFolder)?.renameTo(fileFactory.folderFrom(newFolder)) ?: false
        }.getOrNull()
    } ?: false

    suspend fun renameFile(
        folder: Folder,
        oldFilename: Filename,
        newFilename: Filename
    ) = fileOperationIn(folder) {
        runCatching {
            fileFactory.fileFromWhenExists(folder, oldFilename)
                ?.renameTo(fileFactory.fileFrom(folder, newFilename))
                ?: false
        }.getOrNull()
    } ?: false

    suspend fun writeToFile(
        folder: Folder,
        filename: Filename,
        content: String
    ): Boolean = writeToFile(folder, filename, content.toByteArray()) != null

    suspend fun writeToFile(
        folder: Folder,
        filename: Filename,
        content: ByteArray
    ): File? {
        return fileOperationIn(folder) {
            val fileToSave = fileFactory.fileFrom(folder, filename)
            val result = runCatching { fileStreamFactory.outputStreamFrom(fileToSave).use { it.write(content) } }
            when (result.isSuccess) {
                true -> fileToSave
                false -> null
            }
        }
    }

    suspend fun writeToFileAsStream(
        folder: Folder,
        filename: Filename,
        inputStream: InputStream
    ): File? {
        return fileOperationIn(folder) {
            val fileToSave = fileFactory.fileFrom(folder, filename)
            val result = runCatching {
                inputStream.use { input ->
                    fileStreamFactory.outputStreamFrom(fileToSave).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            when (result.isSuccess) {
                true -> fileToSave
                false -> null
            }
        }
    }

    suspend fun copyFile(
        sourceFolder: Folder,
        sourceFilename: Filename,
        targetFolder: Folder,
        targetFilename: Filename
    ): File? = fileOperationIn(sourceFolder, targetFolder) {
        runCatching {
            fileFactory.fileFrom(sourceFolder, sourceFilename)
                .copyTo(fileFactory.fileFrom(targetFolder, targetFilename))
        }.getOrNull()
    }

    suspend fun deleteFile(folder: Folder, filename: Filename): Boolean = fileOperationIn(folder) {
        runCatching {
            fileFactory.fileFrom(folder, filename).delete()
        }.getOrNull()
    } ?: false

    suspend fun deleteFolder(folder: Folder): Boolean = fileOperationIn(folder) {
        runCatching {
            fileFactory.folderFrom(folder).deleteRecursively()
        }.getOrNull()
    } ?: false

    private suspend fun <T> fileOperationIn(vararg folders: Folder, operation: () -> T): T? =
        withContext(dispatcherProvider.Io) {
            if (folders.any { it.isBlacklisted() }) {
                Timber.w("Trying to access a blacklisted file directory: ${folders.map { it.path }}")
                null
            } else {
                operation()
            }
        }

    private fun Folder.isBlacklisted(): Boolean {
        val internalAppFiles = applicationContext.filesDir
        val blacklistedFolders = listOf(
            "${internalAppFiles.parent}/databases",
            "${internalAppFiles.parent}/shared_prefs",
            "${internalAppFiles.path}/datastore"
        )
        return blacklistedFolders.any { blacklistedFolder -> path.normalised() == blacklistedFolder.normalised() }
    }

    private fun String.normalised() = File(this).normalize().path

    data class Folder(val path: String)

    data class Filename(val value: String)

    class FileStreamFactory @Inject constructor() {

        fun inputStreamFrom(file: File): InputStream = FileInputStream(file)

        fun outputStreamFrom(file: File): OutputStream = FileOutputStream(file)
    }

    class FileFactory @Inject constructor() {

        fun fileFrom(folder: Folder, filename: Filename) = File(
            folderFrom(folder),
            filename.value
        )

        fun fileFromWhenExists(folder: Folder, filename: Filename) = folderFromWhenExists(folder)?.let { dir ->
            File(dir, filename.value).takeIf { it.exists() }
        }

        fun folderFrom(folder: Folder) = File(folder.path).apply { mkdirs() }

        fun folderFromWhenExists(folder: Folder) = File(folder.path).takeIf { it.exists() }
    }
}
