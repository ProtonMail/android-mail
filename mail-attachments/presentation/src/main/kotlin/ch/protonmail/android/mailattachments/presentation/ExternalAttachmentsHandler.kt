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

import android.net.Uri
import arrow.core.Either

interface ExternalAttachmentsHandler {

    suspend fun copyUriToDestination(sourceUri: Uri, destinationUri: Uri): Either<ExternalAttachmentErrorResult, Unit>

    suspend fun saveFileToDownloadsFolder(
        sourceUri: Uri,
        fileName: String,
        mimeType: String
    ): Either<ExternalAttachmentErrorResult, Unit>

    suspend fun saveDataToDestination(
        destinationUri: Uri,
        mimeType: String,
        data: ByteArray
    ): Either<ExternalAttachmentErrorResult, Unit>

    suspend fun saveDataToDownloads(
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Either<ExternalAttachmentErrorResult, Unit>
}

sealed interface ExternalAttachmentErrorResult {
    data object UnableToCreateUri : ExternalAttachmentErrorResult
    data object UnableToCopy : ExternalAttachmentErrorResult
    data object SourceUnavailable : ExternalAttachmentErrorResult
    data object UnableToLoadImage : ExternalAttachmentErrorResult
    data object UserNotFound : ExternalAttachmentErrorResult
}
