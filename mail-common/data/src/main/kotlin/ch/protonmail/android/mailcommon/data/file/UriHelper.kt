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
import java.io.InputStream
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class UriHelper @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val contentResolverHelper: ContentResolverHelper
) {

    suspend fun readFromUri(uri: Uri): InputStream? = withContext(dispatcherProvider.Io) {
        runCatching { contentResolverHelper.openInputStream(uri) }.getOrElse {
            Timber.e(it, "uri-helper: Could not read from uri")
            null
        }
    }

    suspend fun resolveFileInformation(uri: Uri, file: File): FileInformation? {
        val name = getFileNameFromUri(uri)
        val size = getFileSizeFromUri(uri)
        val mimeType = getFileMimeTypeFromUri(uri)

        if (name.isNullOrEmpty() || size == null || mimeType.isNullOrEmpty()) {
            Timber.e("uri-helper: Could not resolve file information")
            return null
        }

        return FileInformation(
            name = name,
            path = file.path,
            size = size,
            mimeType = mimeType
        )
    }

    suspend fun getFileNameFromUri(uri: Uri) = withContext(dispatcherProvider.Io) {
        contentResolverHelper.query(uri)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)?.sanitisedAsFileName()
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
    val path: String,
    val size: Long,
    val mimeType: String
)

/**
 * [OpenableColumns.DISPLAY_NAME] is supplied by whichever provider serves the uri, so for a shared file it is
 * attacker controlled. It ends up as the attachment name on an outgoing message, hence path separators, traversal
 * segments, control characters and bidi/zero width format characters are stripped before it is used.
 */
private fun String.sanitisedAsFileName(): String {
    val sanitised = substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(NON_PRINTABLE_CHARACTERS, "")
        .trim()
        .truncatedPreservingExtension()

    return if (sanitised.isEmpty() || sanitised.all { it == '.' }) "" else sanitised
}

/**
 * Keeps the extension when shortening an over long name, so the attachment is still presented as the kind of file
 * it actually is. The mime type is resolved from the provider rather than from the name, so this is presentation
 * only.
 */
private fun String.truncatedPreservingExtension(): String {
    if (length <= MAX_FILE_NAME_LENGTH) return this

    val extension = substringAfterLast('.', missingDelimiterValue = "")
    if (extension.isEmpty() || extension.length > MAX_EXTENSION_LENGTH) return take(MAX_FILE_NAME_LENGTH)

    val suffix = ".$extension"
    return take(MAX_FILE_NAME_LENGTH - suffix.length) + suffix
}

private val NON_PRINTABLE_CHARACTERS = Regex("[\\p{Cc}\\p{Cf}]")
private const val MAX_EXTENSION_LENGTH = 16

private const val MAX_FILE_NAME_LENGTH = 255

