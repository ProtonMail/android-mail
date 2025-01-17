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

package ch.protonmail.android.mailbugreport.data.provider

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbugreport.domain.provider.LogcatProvider
import ch.protonmail.android.mailbugreport.domain.provider.LogcatProviderError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class LogcatProviderImpl @Inject constructor(
    @ApplicationContext val context: Context
) : LogcatProvider {

    override fun getParentPath(): File = File(context.cacheDir, LogsDirName)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getLogcatFile(): Either<LogcatProviderError, File> = withContext(Dispatchers.IO) {
        runCatching {
            val logDir = getParentPath()
            if (!logDir.exists()) logDir.mkdirs()

            val packageName = context.packageName
            val logFile = File(logDir, "logcat_$packageName.txt")

            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "time", "*:V", "--t", getCutoffTimestamp())
            )

            process.inputStream.bufferedReader().useLines { lines ->
                logFile.bufferedWriter().use { writer ->
                    lines.forEach { line ->
                        writer.appendLine(line)
                    }
                }
            }

            logFile.right()
        }.getOrElse { e ->
            Timber.e(e, "Error dumping logcat")
            LogcatProviderError.Error.left()
        }
    }

    private fun getCutoffTimestamp(): String {
        val cutoffInstant = Instant.now().minusSeconds(CutOffTimePeriod)
        val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.000").withZone(ZoneId.systemDefault())
        return formatter.format(cutoffInstant)
    }

    private companion object {

        const val CutOffTimePeriod = 12 * 3600L // 12 hours
        const val LogsDirName = "logcat"
    }
}
