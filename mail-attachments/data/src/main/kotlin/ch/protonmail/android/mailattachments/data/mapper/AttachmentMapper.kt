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
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailattachments.domain.model.AttachmentMimeType
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailattachments.domain.model.ConvertAttachmentError
import ch.protonmail.android.mailattachments.domain.model.MimeTypeCategory
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentDisposition
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMimeType
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeTypeCategory
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import uniffi.mail_uniffi.DraftAttachmentDispositionSwapError
import uniffi.mail_uniffi.DraftAttachmentDispositionSwapErrorReason
import uniffi.mail_uniffi.DraftAttachmentError
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
}

fun DraftAttachmentUploadError.toAttachmentError(): AddAttachmentError = when (this) {
    is DraftAttachmentUploadError.Other -> AddAttachmentError.Other(this.v1.toDataError())
    is DraftAttachmentUploadError.Reason -> when (this.v1) {
        DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST,
        DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST_ON_SERVER,
        DraftAttachmentUploadErrorReason.MESSAGE_ALREADY_SENT -> AddAttachmentError.InvalidDraftMessage

        DraftAttachmentUploadErrorReason.CRYPTO -> AddAttachmentError.EncryptionError
        DraftAttachmentUploadErrorReason.ATTACHMENT_TOO_LARGE -> AddAttachmentError.AttachmentTooLarge
        DraftAttachmentUploadErrorReason.TOO_MANY_ATTACHMENTS -> AddAttachmentError.TooManyAttachments
        DraftAttachmentUploadErrorReason.RETRY_INVALID_STATE -> AddAttachmentError.InvalidState
        DraftAttachmentUploadErrorReason.TOTAL_ATTACHMENT_SIZE_TOO_LARGE -> AddAttachmentError.AttachmentTooLarge
        DraftAttachmentUploadErrorReason.TIMEOUT -> AddAttachmentError.UploadTimeout
        DraftAttachmentUploadErrorReason.STORAGE_QUOTA_EXCEEDED -> AddAttachmentError.StorageQuotaExceeded
    }
}

fun DraftAttachmentDispositionSwapError.Reason.toConvertAttachmentError() = when (this.v1) {
    DraftAttachmentDispositionSwapErrorReason.INVALID_STATE -> ConvertAttachmentError.InvalidState

    DraftAttachmentDispositionSwapErrorReason.NOOP -> ConvertAttachmentError.Noop

    DraftAttachmentDispositionSwapErrorReason.ATTACHMENT_DOES_NOT_EXIST -> ConvertAttachmentError.AttachmentNotExisting

    DraftAttachmentDispositionSwapErrorReason.ATTACHMENT_MESSAGE_DOES_NOT_EXIST ->
        ConvertAttachmentError.MessageNotExisting

    DraftAttachmentDispositionSwapErrorReason.ATTACHMENT_MESSAGE_IS_NOT_A_DRAFT ->
        ConvertAttachmentError.AttachmentMessageIsNotADraft
}
