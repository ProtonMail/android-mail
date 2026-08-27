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

package ch.protonmail.android.mailbugreport.domain.usecase

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.content.Context
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.AppLogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.LogsExportFeatureSettingValue
import ch.protonmail.android.mailbugreport.domain.annotations.RustLogsFileHandler
import ch.protonmail.android.mailbugreport.domain.helper.EventsFileNameHelper
import ch.protonmail.android.mailbugreport.domain.provider.LogcatProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAggregatedEventsZipFile @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val logcatProvider: LogcatProvider,
    @AppLogsFileHandler private val appLogsFileHandler: LogsFileHandler,
    @RustLogsFileHandler private val rustLogsFileHandler: LogsFileHandler,
    @LogsExportFeatureSettingValue private val logsExportFeatureSetting: LogsExportFeatureSetting
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        runCatching {
            val directoriesList = buildDirectoriesList(logsExportFeatureSetting)
            val fileName = EventsFileNameHelper.generateTimestampedFilename()
            // Kept inside a dedicated sub directory so the FileProvider does not need a root over the whole cache.
            val outputDirectory = File(applicationContext.cacheDir, ExportDirName).apply { mkdirs() }
            val outputFile = File(outputDirectory, fileName).also {
                if (it.exists()) it.delete()
            }

            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    directoriesList.forEach { file ->
                        zipFileOrDirectory(file, zos)
                    }
                }
            }
            outputFile
        }
    }

    private fun zipFileOrDirectory(
        file: File,
        zos: ZipOutputStream,
        baseName: String = ""
    ) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                zipFileOrDirectory(child, zos, "$baseName${file.name}/")
            }
        } else {
            FileInputStream(file).use { fis ->
                val entryName = "$baseName${file.name}"
                zos.putNextEntry(ZipEntry(entryName))
                fis.copyTo(zos, bufferSize = 1024)
            }
        }
    }

    private suspend fun buildDirectoriesList(logsExportFeatureSetting: LogsExportFeatureSetting): List<File> {
        return buildList {
            // Attaching logcat is only for internal QA, this is not enabled for end users.
            if (logsExportFeatureSetting.isInternalFeatureEnabled) {
                logcatProvider.getLogcatFile()
                add(logcatProvider.getParentPath())
            }
            add(appLogsFileHandler.getParentPath())
            add(rustLogsFileHandler.getParentPath())
        }
    }

    private companion object {

        const val ExportDirName = "export_logs"
    }
}
