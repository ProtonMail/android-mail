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

package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import arrow.core.Either
import arrow.core.flatMap
import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import ch.protonmail.android.mailcomposer.domain.usecase.AddInlineAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.AddStandardAttachment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AddAttachment @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val addStandardAttachment: AddStandardAttachment,
    private val addInlineAttachment: AddInlineAttachment,
    private val pastedImageRepository: PastedImageRepository
) {

    suspend operator fun invoke(
        fileUri: Uri,
        draftMimeType: DraftMimeType
    ): Either<AddAttachmentError, AddAttachmentResult> {
        val isNotImageMimeType = fileUri.mimeType() !in imageMimeTypes()
        val draftMimeIsPlainText = draftMimeType == DraftMimeType.PlainText

        if (isNotImageMimeType || draftMimeIsPlainText) {
            return addStandardAttachment(fileUri)
                .map { AddAttachmentResult.StandardAttachmentAdded }
        }

        return addInlineAttachment(fileUri)
            .map { AddAttachmentResult.InlineAttachmentAdded(it) }
    }

    suspend fun forcingStandardDisposition(
        fileUri: Uri
    ): Either<AddAttachmentError, AddAttachmentResult.StandardAttachmentAdded> {
        return addStandardAttachment(fileUri)
            .map { AddAttachmentResult.StandardAttachmentAdded }
    }

    /**
     * Adds an image pasted into the editor (received as base64) by first storing it as a temporary file, then
     * routing it through [invoke] so it is added as an inline attachment just like a picked or captured image.
     */
    suspend fun fromPastedImage(
        base64: String,
        draftMimeType: DraftMimeType
    ): Either<AddAttachmentError, AddAttachmentResult> = pastedImageRepository.saveBase64ImageToCache(base64)
        .mapLeft { AddAttachmentError.Other(it) }
        .flatMap { file ->
            val fileUri = FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.provider",
                file
            )
            invoke(fileUri, draftMimeType)
        }

    private fun imageMimeTypes() = listOf(
        "image/jpg",
        "image/webp",
        "image/jpeg",
        "image/gif",
        "image/apng",
        "image/png"
    )

    private fun Uri.mimeType() = applicationContext.contentResolver.getType(this)

    sealed interface AddAttachmentResult {
        data object StandardAttachmentAdded : AddAttachmentResult
        data class InlineAttachmentAdded(val cid: String) : AddAttachmentResult
    }
}
