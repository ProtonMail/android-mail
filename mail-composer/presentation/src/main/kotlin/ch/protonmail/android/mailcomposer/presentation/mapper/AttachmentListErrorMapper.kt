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

    private val priorityOrder = listOf(
        AddAttachmentError.StorageQuotaExceeded,
        AddAttachmentError.TooManyAttachments,
        AddAttachmentError.AttachmentTooLarge,
        AddAttachmentError.InvalidDraftMessage,
        AddAttachmentError.EncryptionError,
        AddAttachmentError.UploadTimeout,
        AddAttachmentError.InvalidState
    )

    fun toAttachmentAddErrorWithList(attachments: List<AttachmentMetadataWithState>): AttachmentAddErrorWithList? {
        val errored = attachments.filter { it.attachmentState is AttachmentState.Error }
        if (errored.isEmpty()) return null

        val byError = errored.groupBy { (it.attachmentState as AttachmentState.Error).addAttachmentErrorOrNull() }

        priorityOrder.forEach { error ->
            byError[error]?.let { return AttachmentAddErrorWithList(error, it) }
        }

        return AttachmentAddErrorWithList(AddAttachmentError.Other(DataError.Local.Unknown), errored)
    }

    private fun AttachmentState.Error.addAttachmentErrorOrNull(): AddAttachmentError? =
        (reason as? AttachmentError.AddAttachment)?.error
}
