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

package ch.protonmail.android.mailbugreport.data

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LogsFileHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val coroutineDispatcher: CoroutineDispatcher
) : LogsFileHandler, CoroutineScope {

    override val coroutineContext: CoroutineContext = coroutineDispatcher + SupervisorJob()

    private val logDir by lazy {
        File(context.cacheDir, LogsDirName)
            .apply { mkdirs() }
    }

    private var currentLogFile: File? = null
    private var fileWriter: FileWriter? = null

    override fun getParentPath(): File = logDir

    override fun getLastLogFile(): File? = currentLogFile

    override fun writeLog(message: String) = launch {
        runCatching {
            (fileWriter ?: prepareFileWriter()).let { writer ->
                writer.appendLine(message)
                writer.flush()
            }

            currentLogFile?.takeIf { it.length() > MaxFileSizeBytes }?.let {
                rotateLogFiles()
            }
        }.onFailure { it.printStackTrace() }
    }.let {}

    private fun prepareFileWriter(): FileWriter {
        val files = logDir.listFiles().orEmpty().sortedBy { it.lastModified() }

        currentLogFile = when {
            files.isEmpty() -> createNewFile()
            files.last().length() > MaxFileSizeBytes -> createNewFile()
            else -> files.last()
        }

        return FileWriter(currentLogFile, true).also { fileWriter = it }
    }

    private fun rotateLogFiles() {
        val existingFiles = logDir.listFiles().orEmpty()
            .sortedBy { it.lastModified() }

        if (existingFiles.size >= MaxLogFiles) {
            existingFiles
                .take(existingFiles.size - MaxLogFiles + 1)
                .forEach { it.delete() }
        }

        prepareFileWriter()
    }

    private fun createNewFile(): File {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val newFile = File(logDir, "log-$date.txt")

        runCatching {
            newFile.createNewFile()
        }.onFailure { it.printStackTrace() }

        return newFile
    }

    override fun close() {
        cancel()
        runCatching {
            fileWriter?.close()
        }.onFailure { it.printStackTrace() }
    }

    companion object {

        private const val LogsDirName = "logs"
        private const val MaxFileSizeBytes = 2 * 1024 * 1024 // 2 MB
        private const val MaxLogFiles = 3
    }
}
