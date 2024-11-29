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

package ch.protonmail.android.mailbugreport.presentation.utils

import java.io.File
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object ApplicationLogsUtils {
    private const val FileProviderSuffix = ".logsfileprovider"

    suspend fun Context.shareLogs(file: File) {
        val fileUri = withContext(Dispatchers.IO) {
            FileProvider.getUriForFile(
                this@shareLogs,
                "$packageName$FileProviderSuffix",
                file
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            clipData = ClipData.newRawUri("", fileUri)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share File"))
    }
}
