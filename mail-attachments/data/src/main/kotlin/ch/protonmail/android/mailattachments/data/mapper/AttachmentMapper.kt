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

package ch.protonmail.android.mailattachments.data.mapper

import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentDisposition
import ch.protonmail.android.mailattachments.domain.model.AttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentError.RemoveAttachment
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailattachments.domain.model.AttachmentMimeType
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailattachments.domain.model.ConvertAttachmentError
import ch.protonmail.android.mailattachments.domain.model.MimeTypeCategory
import ch.protonmail.android.mailattachments.domain.model.RemoveAttachmentError
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentDisposition
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMimeType
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeTypeCategory
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.DraftAttachmentDispositionSwapError
import uniffi.mail_uniffi.DraftAttachmentDispositionSwapErrorReason
import uniffi.mail_uniffi.DraftAttachmentError
import uniffi.mail_uniffi.DraftAttachmentRemoveError
import uniffi.mail_uniffi.DraftAttachmentRemoveErrorReason
import uniffi.mail_uniffi.DraftAttachmentState
import uniffi.mail_uniffi.DraftAttachmentUploadError
import uniffi.mail_uniffi.DraftAttachmentUploadErrorReason

fun LocalAttachmentId.toAttachmentId(): AttachmentId = AttachmentId(this.value.toString())
fun AttachmentId.toLocalAttachmentId(): LocalAttachmentId = LocalAttachmentId(this.id.toULong())

fun List<LocalAttachmentMetadata>.getCalendarAttachmentCount(): Int =
    this.filter { it.mimeType.category == LocalMimeTypeCategory.CALENDAR }.size

fun LocalAttachmentMetadata.toAttachmentMetadata(): AttachmentMetadata {
    return AttachmentMetadata(
        attachmentId = this.id.toAttachmentId(),
        name = this.name,
        size = this.size.toLong(),
        mimeType = this.mimeType.toAttachmentMimeType(),
        disposition = this.disposition.toAttachmentDisposition(),
        includeInPreview = this.isListable
    )
}

fun LocalAttachmentDisposition.toAttachmentDisposition(): AttachmentDisposition {
    return when (this) {
        LocalAttachmentDisposition.INLINE -> AttachmentDisposition.Inline
        LocalAttachmentDisposition.ATTACHMENT -> AttachmentDisposition.Attachment
    }
}

fun LocalAttachmentMimeType.toAttachmentMimeType(): AttachmentMimeType {
    return AttachmentMimeType(
        mime = this.mime,
        category = this.category.toMimeTypeCategory()
    )
}

fun LocalMimeTypeCategory.toMimeTypeCategory(): MimeTypeCategory = when (this) {
    LocalMimeTypeCategory.AUDIO -> MimeTypeCategory.Audio
    LocalMimeTypeCategory.CALENDAR -> MimeTypeCategory.Calendar
    LocalMimeTypeCategory.CODE -> MimeTypeCategory.Code
    LocalMimeTypeCategory.COMPRESSED -> MimeTypeCategory.Compressed
    LocalMimeTypeCategory.DEFAULT -> MimeTypeCategory.Default
    LocalMimeTypeCategory.EXCEL -> MimeTypeCategory.Excel
    LocalMimeTypeCategory.FONT -> MimeTypeCategory.Font
    LocalMimeTypeCategory.IMAGE -> MimeTypeCategory.Image
    LocalMimeTypeCategory.KEY -> MimeTypeCategory.Key
    LocalMimeTypeCategory.KEYNOTE -> MimeTypeCategory.Keynote
    LocalMimeTypeCategory.NUMBERS -> MimeTypeCategory.Numbers
    LocalMimeTypeCategory.PAGES -> MimeTypeCategory.Pages
    LocalMimeTypeCategory.PDF -> MimeTypeCategory.Pdf
    LocalMimeTypeCategory.POWERPOINT -> MimeTypeCategory.Powerpoint
    LocalMimeTypeCategory.TEXT -> MimeTypeCategory.Text
    LocalMimeTypeCategory.VIDEO -> MimeTypeCategory.Video
    LocalMimeTypeCategory.WORD -> MimeTypeCategory.Word
    LocalMimeTypeCategory.UNKNOWN -> MimeTypeCategory.Unknown
}

fun DraftAttachmentState.toAttachmentState(): AttachmentState = when (this) {
    DraftAttachmentState.Uploaded -> AttachmentState.Uploaded
    DraftAttachmentState.Uploading -> AttachmentState.Uploading
    DraftAttachmentState.Pending -> AttachmentState.Pending
    DraftAttachmentState.Offline -> AttachmentState.Pending
    is DraftAttachmentState.Error -> AttachmentState.Error(this.v1.toDraftAttachmentError())
}

fun DraftAttachmentError.toDraftAttachmentError(): AttachmentError = when (val result = this) {
    is DraftAttachmentError.DispositionSwap -> when (val result = this.v1) {
        is DraftAttachmentDispositionSwapError.Other ->
            AttachmentError.AddAttachment(AddAttachmentError.Other(result.v1.toDataError()))

        is DraftAttachmentDispositionSwapError.Reason ->
            AttachmentError.ConvertInlineToAttachment(result.toConvertAttachmentError())
    }

    is DraftAttachmentError.Upload -> AttachmentError.AddAttachment(result.v1.toAttachmentError())
    is DraftAttachmentError.Remove -> RemoveAttachment(result.v1.toRemoveAttachmentError())
}

fun DraftAttachmentUploadError.toAttachmentError(): AddAttachmentError = when (this) {
    is DraftAttachmentUploadError.Other -> AddAttachmentError.Other(this.v1.toDataError())
    is DraftAttachmentUploadError.Reason -> {
        val reason = this.v1
        when (reason) {
            DraftAttachmentUploadErrorReason.MessageDoesNotExist,
            DraftAttachmentUploadErrorReason.MessageDoesNotExistOnServer,
            DraftAttachmentUploadErrorReason.MessageAlreadySent -> AddAttachmentError.InvalidDraftMessage

            DraftAttachmentUploadErrorReason.Crypto -> AddAttachmentError.EncryptionError
            DraftAttachmentUploadErrorReason.AttachmentTooLarge -> AddAttachmentError.AttachmentTooLarge
            DraftAttachmentUploadErrorReason.TooManyAttachments -> AddAttachmentError.TooManyAttachments
            DraftAttachmentUploadErrorReason.RetryInvalidState -> AddAttachmentError.InvalidState
            DraftAttachmentUploadErrorReason.TotalAttachmentSizeTooLarge -> AddAttachmentError.AttachmentTooLarge
            DraftAttachmentUploadErrorReason.Timeout -> AddAttachmentError.UploadTimeout
            DraftAttachmentUploadErrorReason.StorageQuotaExceeded -> AddAttachmentError.StorageQuotaExceeded
            is DraftAttachmentUploadErrorReason.BadRequest -> AddAttachmentError.Other(DataError.Remote.BadRequest)
        }
    }
}

fun DraftAttachmentDispositionSwapError.Reason.toConvertAttachmentError(): ConvertAttachmentError {
    return when (this.v1) {
        DraftAttachmentDispositionSwapErrorReason.InvalidState -> ConvertAttachmentError.InvalidState

        DraftAttachmentDispositionSwapErrorReason.Noop -> ConvertAttachmentError.Noop

        DraftAttachmentDispositionSwapErrorReason.AttachmentDoesNotExist -> ConvertAttachmentError.AttachmentNotExisting

        DraftAttachmentDispositionSwapErrorReason.AttachmentMessageDoesNotExist ->
            ConvertAttachmentError.MessageNotExisting

        DraftAttachmentDispositionSwapErrorReason.AttachmentMessageIsNotADraft ->
            ConvertAttachmentError.AttachmentMessageIsNotADraft

        is DraftAttachmentDispositionSwapErrorReason.BadRequest ->
            ConvertAttachmentError.Other(DataError.Remote.BadRequest)
    }
}

fun DraftAttachmentRemoveError.toRemoveAttachmentError() = when (this) {
    is DraftAttachmentRemoveError.Other -> RemoveAttachmentError.Other(this.v1.toDataError())
    is DraftAttachmentRemoveError.Reason -> when (val result = this.v1) {
        DraftAttachmentRemoveErrorReason.AttachmentDoesNotExist -> RemoveAttachmentError.AttachmentDoesNotExist
        is DraftAttachmentRemoveErrorReason.BadRequest -> RemoveAttachmentError.BadRequest(result.v1)
    }
}
