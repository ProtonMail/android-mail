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
import ch.protonmail.android.mailattachments.presentation.usecase.GenerateUniqueFileName
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
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
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = CopyBufferSize)
                    }
                } ?: raise(ExternalAttachmentErrorResult.UnableToCopy)
            } catch (exception: IOException) {
                // The source file lives in the app cache and can be evicted (by the OS or by the SDK)
                // while the user is picking a destination, so a missing file is an expected failure here.
                Timber.w(exception, "Unable to copy the file to the selected destination.")
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            } catch (exception: SecurityException) {
                Timber.w(exception, "Not allowed to access the source or the destination file.")
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            }
        }
    }

    override suspend fun saveFileToDownloadsFolder(
        sourceUri: Uri,
        fileName: String,
        mimeType: String
    ): Either<ExternalAttachmentErrorResult, Unit> = either {
        withContext(ioDispatcher + NonCancellable) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, generateUniqueFileName(fileName))
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: raise(ExternalAttachmentErrorResult.UnableToCreateUri)

            copyUriToDestination(sourceUri = sourceUri, destinationUri = uri)
                .onLeft { deleteQuietly(uri) }
                .bind()
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
            } catch (exception: IOException) {
                Timber.w(exception, "Unable to write the data to the selected destination.")
                raise(ExternalAttachmentErrorResult.UnableToCopy)
            } catch (exception: SecurityException) {
                Timber.w(exception, "Not allowed to write to the selected destination.")
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

            saveDataToDestination(destinationUri = uri, mimeType = mimeType, data = data)
                .onLeft { deleteQuietly(uri) }
                .bind()
        }
    }

    private fun deleteQuietly(uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
            .onFailure { Timber.w(it, "Unable to delete the incomplete entry at $uri.") }
    }

    private companion object {

        const val CopyBufferSize = 8 * 1024
    }
}
