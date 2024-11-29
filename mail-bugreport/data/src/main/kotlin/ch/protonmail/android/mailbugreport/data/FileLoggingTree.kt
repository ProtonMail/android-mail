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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import timber.log.Timber

class FileLoggingTree(private val logsFileHandler: LogsFileHandler) : Timber.Tree() {

    init {
        initNewSession()
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (tag in ExcludedTags) return
        val logMessage = createLogMessage(priority, tag, message)
        logsFileHandler.writeLog(logMessage)
    }

    private fun initNewSession() {
        Timber.tag("FileLoggingTree").i(
            """
                |
                |---------- New Session ----------
                |
            """.trimMargin()
        )
    }

    private fun createLogMessage(
        priority: Int,
        tag: String?,
        message: String
    ): String {
        val priorityTag = priorityChar(priority)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        return "$timestamp $priorityTag/$tag: $message"
    }

    private fun priorityChar(priority: Int): Char {
        return when (priority) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            Log.ASSERT -> 'A'
            else -> '?'
        }
    }

    private companion object {

        val ExcludedTags = listOf(
            "core.network"
        )
    }
}
