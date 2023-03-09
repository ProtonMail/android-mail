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
import ch.protonmail.android.mailcommon.domain.util.kilobytes
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class FileHelper @Inject constructor(
    private val fileStreamFactory: FileStreamFactory,
    private val fileFactory: FileFactory,
    private val dispatcherProvider: DispatcherProvider
) {

    suspend fun readFromFile(folder: Folder, filename: Filename): String? = withContext(dispatcherProvider.Io) {
        val fileToRead = fileFactory.fileFrom(folder, filename)
        runCatching {
            fileStreamFactory.inputStreamFrom(fileToRead)
                .bufferedReader()
                .use { it.readText() }
        }.getOrNull()
    }

    suspend fun writeToFile(
        folder: Folder,
        filename: Filename,
        content: String
    ): Boolean = withContext(dispatcherProvider.Io) {
        val fileToSave = fileFactory.fileFrom(folder, filename)
        runCatching {
            fileStreamFactory.outputStreamFrom(fileToSave).use {
                it.write(content.toByteArray())
            }
        }.isSuccess
    }

    suspend fun deleteFile(folder: Folder, filename: Filename) = withContext(dispatcherProvider.Io) {
        runCatching {
            fileFactory.fileFrom(folder, filename).delete()
        }.getOrDefault(false)
    }

    suspend fun deleteFolder(folder: Folder) = withContext(dispatcherProvider.Io) {
        runCatching {
            fileFactory.folderFrom(folder).deleteRecursively()
        }.getOrDefault(false)
    }

    @JvmInline
    value class Folder(val path: String)

    @JvmInline
    value class Filename(val value: String)

    class FileStreamFactory @Inject constructor() {

        fun inputStreamFrom(file: File): InputStream = FileInputStream(file)

        fun outputStreamFrom(file: File): OutputStream = FileOutputStream(file)
    }

    class FileFactory @Inject constructor() {

        fun fileFrom(folder: Folder, filename: Filename) = File(
            folderFrom(folder),
            filename.value
        )

        fun folderFrom(folder: Folder) = File(folder.path).apply { mkdirs() }
    }
}

/**
 * See https://www.sqlite.org/intern-v-extern-blob.html
 *
 * The article compares the performance of storing large blobs in db directly vs storing them as files on disk.
 * It does not directly relate to Android, and no actual testing was done on Android from our side.
 * The assumption is that the performance findings will roughly translate to Android nevertheless and the cut-off
 * threshold is chosen based on the measurements from the article.
 */
fun String.shouldBeStoredAsFile() = this.toByteArray().size > 500.kilobytes
