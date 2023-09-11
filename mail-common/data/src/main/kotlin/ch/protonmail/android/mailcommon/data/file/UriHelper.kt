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

import java.io.InputStream
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class UriHelper @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val contentResolverHelper: ContentResolverHelper
) {

    suspend fun readFromUri(uri: Uri): InputStream? = withContext(dispatcherProvider.Io) {
        runCatching { contentResolverHelper.openInputStream(uri) }.getOrNull()
    }

    suspend fun getFileInformationFromUri(uri: Uri): FileInformation? {
        val name = getFileNameFromUri(uri)
        val size = getFileSizeFromUri(uri)
        val mimeType = getFileMimeTypeFromUri(uri)

        if (name.isNullOrEmpty() || size == null || mimeType.isNullOrEmpty()) return null

        return FileInformation(name, size, mimeType)
    }

    private suspend fun getFileNameFromUri(uri: Uri) = withContext(dispatcherProvider.Io) {
        contentResolverHelper.query(uri)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
    }

    private suspend fun getFileSizeFromUri(uri: Uri) = withContext(dispatcherProvider.Io) {
        contentResolverHelper.query(uri)
            ?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            }
    }

    private suspend fun getFileMimeTypeFromUri(uri: Uri) = withContext(dispatcherProvider.Io) {
        contentResolverHelper.getType(uri)
    }
}

class ContentResolverHelper @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {

    fun openInputStream(uri: Uri) = applicationContext.contentResolver.openInputStream(uri)

    fun getType(uri: Uri) = applicationContext.contentResolver.getType(uri)

    fun query(uri: Uri) = applicationContext.contentResolver.query(uri, null, null, null, null)
}

data class FileInformation(
    val name: String,
    val size: Long,
    val mimeType: String
)
