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

package ch.protonmail.android.logging

import java.io.File
import java.io.IOException
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.report.domain.provider.BugReportLogProvider
import me.proton.core.util.kotlin.HashUtils
import timber.log.Timber
import javax.inject.Inject

class BugReportLogProviderImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val accountManager: AccountManager
) : BugReportLogProvider {

    override suspend fun getLog(): File? {

        val userId = accountManager.getPrimaryUserId().firstOrNull()

        if (userId == null) {
            Timber.e("could not get PrimaryUserId in BugReportLogProviderImpl")
            return null
        }

        return try {

            File(logDirectoryName(userId)).apply { mkdirs() }

            val logFileName = "${System.currentTimeMillis()}.log"
            val logFile = File(logDirectoryName(userId), logFileName).apply { this.createNewFile() }

            // dump the log to a file
            Runtime.getRuntime().exec("logcat -d -v time -f " + logFile.absolutePath)

            logFile

        } catch (e: IOException) {
            Timber.e(e, "exception creating log file for bug report")
            null
        } catch (e: SecurityException) {
            Timber.e(e, "exception creating log file for bug report")
            null
        }
    }

    override suspend fun releaseLog(log: File) {}

    private fun logDirectoryName(userId: UserId): String =
        "${applicationContext.cacheDir}/${HashUtils.sha256(userId.id)}/logs"

}
