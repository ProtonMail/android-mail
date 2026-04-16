/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation

import java.io.IOException
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.usecase.GenerateUniqueFileName
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExternalAttachmentsHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val generateUniqueFileName: GenerateUniqueFileName
) : ExternalAttachmentsHandler {

    override suspend fun copyUriToDestination(
        sourceUri: Uri,
        destinationUri: Uri
    ): Either<ExternalAttachmentErrorResult, Unit> = either {
        withContext(ioDispatcher + NonCancellable) {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 8 * 1024)
                }
            } ?: raise(ExternalAttachmentErrorResult.UnableToCopy)
        }
    }

    override suspend fun saveFileToDownloadsFolder(
        fileContent: FileContent
    ): Either<ExternalAttachmentErrorResult, Unit> = either {
        withContext(ioDispatcher + NonCancellable) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, generateUniqueFileName(fileContent.name))
                put(MediaStore.Downloads.MIME_TYPE, fileContent.mimeType)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: raise(ExternalAttachmentErrorResult.UnableToCreateUri)

            try {
                copyUriToDestination(sourceUri = fileContent.uri, destinationUri = uri)
            } catch (_: IOException) {
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            }
        }
    }

    override suspend fun saveDataToDestination(
        destinationUri: Uri,
        mimeType: String,
        data: ByteArray
    ): Either<ExternalAttachmentErrorResult, Unit> = either {
        withContext(ioDispatcher + NonCancellable) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    outputStream.write(data)
                } ?: raise(ExternalAttachmentErrorResult.UnableToCopy)
            } catch (_: IOException) {
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            }
        }
    }

    override suspend fun saveDataToDownloads(
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Either<ExternalAttachmentErrorResult, Unit> = either {
        withContext(ioDispatcher + NonCancellable) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: raise(ExternalAttachmentErrorResult.UnableToCreateUri)
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data)
                }
            } catch (_: IOException) {
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            }
        }
    }
}
