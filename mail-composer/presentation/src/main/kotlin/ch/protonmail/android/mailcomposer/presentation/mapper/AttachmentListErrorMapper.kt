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

package ch.protonmail.android.mailcomposer.presentation.mapper

import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentAddErrorWithList

internal object AttachmentListErrorMapper {

    fun toAttachmentAddErrorWithList(attachments: List<AttachmentMetadataWithState>): AttachmentAddErrorWithList? {
        val itemsWithError: List<Pair<AttachmentMetadataWithState, AttachmentError>> =
            attachments.mapNotNull { item ->
                val errorState = item.attachmentState as? AttachmentState.Error
                errorState?.reason?.let { reason ->
                    item to reason
                }
            }

        if (itemsWithError.isEmpty()) {
            return null
        }

        val storageExceeded = itemsWithError.filterStorageExceededError()
        val tooManyAttachmentItems = itemsWithError.filterTooManyAttachmentsError()
        val attachmentTooLargeItems = itemsWithError.filterAttachmentsTooLargeError()
        val invalidDraftMessageItems = itemsWithError.filterInvalidDraftError()
        val encryptionErrorItems = itemsWithError.filterEncryptionErrorError()

        return if (storageExceeded.isNotEmpty()) {
            AttachmentAddErrorWithList(
                AddAttachmentError.StorageQuotaExceeded,
                storageExceeded.map { it.first }
            )
        } else if (tooManyAttachmentItems.isNotEmpty()) {
            AttachmentAddErrorWithList(
                AddAttachmentError.TooManyAttachments,
                tooManyAttachmentItems.map { it.first }
            )
        } else if (attachmentTooLargeItems.isNotEmpty()) {
            AttachmentAddErrorWithList(
                AddAttachmentError.AttachmentTooLarge,
                attachmentTooLargeItems.map { it.first }
            )
        } else if (invalidDraftMessageItems.isNotEmpty()) {
            AttachmentAddErrorWithList(
                AddAttachmentError.InvalidDraftMessage,
                invalidDraftMessageItems.map { it.first }
            )
        } else if (encryptionErrorItems.isNotEmpty()) {
            AttachmentAddErrorWithList(
                AddAttachmentError.EncryptionError,
                encryptionErrorItems.map { it.first }
            )
        } else {
            AttachmentAddErrorWithList(
                AddAttachmentError.Other(DataError.Local.Unknown),
                itemsWithError.map { it.first }
            )
        }
    }
}

private fun List<Pair<AttachmentMetadataWithState, AttachmentError>>.filterStorageExceededError() = this.filter {
    when (val addAttachment = it.second) {
        is AttachmentError.AddAttachment -> addAttachment.error is AddAttachmentError.StorageQuotaExceeded
        else -> false
    }
}

private fun List<Pair<AttachmentMetadataWithState, AttachmentError>>.filterTooManyAttachmentsError() = this.filter {
    when (val addAttachment = it.second) {
        is AttachmentError.AddAttachment -> addAttachment.error is AddAttachmentError.TooManyAttachments
        else -> false
    }
}

private fun List<Pair<AttachmentMetadataWithState, AttachmentError>>.filterAttachmentsTooLargeError() = this.filter {
    when (val addAttachment = it.second) {
        is AttachmentError.AddAttachment -> addAttachment.error is AddAttachmentError.AttachmentTooLarge
        else -> false
    }
}

private fun List<Pair<AttachmentMetadataWithState, AttachmentError>>.filterInvalidDraftError() = this.filter {
    when (val addAttachment = it.second) {
        is AttachmentError.AddAttachment -> addAttachment.error is AddAttachmentError.InvalidDraftMessage
        else -> false
    }
}

private fun List<Pair<AttachmentMetadataWithState, AttachmentError>>.filterEncryptionErrorError() = this.filter {
    when (val addAttachment = it.second) {
        is AttachmentError.AddAttachment -> addAttachment.error is AddAttachmentError.EncryptionError
        else -> false
    }
}
