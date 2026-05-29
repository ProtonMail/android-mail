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

package ch.protonmail.android.composer.data.mapper

import ch.protonmail.android.composer.data.local.LocalDraft
import ch.protonmail.android.composer.data.local.LocalDraftWithSyncStatus
import ch.protonmail.android.composer.data.wrapper.DraftWrapper
import ch.protonmail.android.composer.data.wrapper.DraftWrapperWithSyncStatus
import ch.protonmail.android.mailattachments.data.mapper.toConvertAttachmentError
import ch.protonmail.android.mailattachments.domain.model.ConvertAttachmentError
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeType
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentDeleteError
import ch.protonmail.android.mailcomposer.domain.model.ChangeSenderError
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftFieldsWithSyncStatus
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.MessagePasswordError
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.ScheduleSendOptions
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import timber.log.Timber
import uniffi.mail_uniffi.DraftAttachmentDispositionSwapError
import uniffi.mail_uniffi.DraftAttachmentUploadError
import uniffi.mail_uniffi.DraftAttachmentUploadErrorReason
import uniffi.mail_uniffi.DraftCreateMode
import uniffi.mail_uniffi.DraftOpenError
import uniffi.mail_uniffi.DraftOpenErrorReason
import uniffi.mail_uniffi.DraftPassword
import uniffi.mail_uniffi.DraftPasswordError
import uniffi.mail_uniffi.DraftPasswordErrorReason
import uniffi.mail_uniffi.DraftScheduleSendOptions
import uniffi.mail_uniffi.DraftSendError
import uniffi.mail_uniffi.DraftSendErrorReason
import uniffi.mail_uniffi.DraftSenderAddressChangeError
import uniffi.mail_uniffi.DraftSenderAddressChangeErrorReason
import uniffi.mail_uniffi.DraftSyncStatus
import uniffi.mail_uniffi.MimeType
import kotlin.time.Instant

fun DraftScheduleSendOptions.toScheduleSendOptions() = ScheduleSendOptions(
    tomorrowTime = Instant.fromEpochSeconds(this.tomorrowTime.toLong()),
    mondayTime = Instant.fromEpochSeconds(this.mondayTime.toLong()),
    isCustomTimeOptionAvailable = this.isCustomOptionAvailable
)

fun LocalDraftWithSyncStatus.toDraftFieldsWithSyncStatus() = when (this) {
    is LocalDraftWithSyncStatus.Local -> DraftFieldsWithSyncStatus.Local(this.localDraft.toDraftFields())
    is LocalDraftWithSyncStatus.Remote -> DraftFieldsWithSyncStatus.Remote(this.localDraft.toDraftFields())
}

fun LocalDraft.toDraftFields() = DraftFields(
    sender = SenderEmail(this.sender),
    subject = Subject(this.subject),
    bodyFields = this.bodyFields,
    mimeType = this.mimeType.toDraftMimeType(),
    recipientsTo = RecipientsTo(this.recipientsTo),
    recipientsCc = RecipientsCc(this.recipientsCc),
    recipientsBcc = RecipientsBcc(this.recipientsBcc)
)

fun DraftWrapperWithSyncStatus.toLocalDraftWithSyncStatus() = when (this.syncStatus) {
    DraftSyncStatus.CACHED -> LocalDraftWithSyncStatus.Local(this.draftWrapper.toLocalDraft())
    DraftSyncStatus.SYNCED -> LocalDraftWithSyncStatus.Remote(this.draftWrapper.toLocalDraft())
}

fun DraftWrapper.toLocalDraft() = LocalDraft(
    subject = this.subject(),
    sender = this.sender(),
    bodyFields = this.bodyFields(),
    mimeType = this.mimeType(),
    recipientsTo = this.recipientsTo().recipients().toComposerRecipients(),
    recipientsCc = this.recipientsCc().recipients().toComposerRecipients(),
    recipientsBcc = this.recipientsBcc().recipients().toComposerRecipients()
)

fun DraftAction.toDraftCreateMode(): DraftCreateMode? = when (this) {
    is DraftAction.Forward -> DraftCreateMode.Forward(this.parentId.toLocalMessageId())
    is DraftAction.Reply -> DraftCreateMode.Reply(this.parentId.toLocalMessageId())
    is DraftAction.ReplyAll -> DraftCreateMode.ReplyAll(this.parentId.toLocalMessageId())
    DraftAction.Compose -> DraftCreateMode.Empty
    is DraftAction.MailTo -> DraftCreateMode.Mailto(this.uri)
    is DraftAction.ComposeToAddresses,
    is DraftAction.ComposeToContactGroup,
    is DraftAction.PrefillForShare -> {
        Timber.e("rust-draft: mapping draft action $this failed! Unsupported by rust DraftCreateMode type")
        null
    }
}

fun LocalAttachmentData.toMessageBodyImage() = MessageBodyImage(this.data, this.mime)

fun DraftSenderAddressChangeError.toChangeSenderError() = when (this) {
    is DraftSenderAddressChangeError.Other -> ChangeSenderError.Other(this.v1.toDataError())
    is DraftSenderAddressChangeError.Reason -> when (val reason = this.v1) {
        is DraftSenderAddressChangeErrorReason.AddressDisabled -> ChangeSenderError.AddressDisabled
        is DraftSenderAddressChangeErrorReason.AddressEmailNotFound -> ChangeSenderError.AddressNotFound(reason.v1)
        is DraftSenderAddressChangeErrorReason.AddressNotSendEnabled -> ChangeSenderError.AddressCanNotSend
    }
}

fun DraftOpenError.toOpenDraftError(): OpenDraftError = when (this) {
    is DraftOpenError.Other -> OpenDraftError.Other(this.v1.toDataError())
    is DraftOpenError.Reason -> when (this.v1) {
        DraftOpenErrorReason.ADDRESS_NOT_FOUND -> OpenDraftError.CouldNotFindAddress
        DraftOpenErrorReason.MESSAGE_BODY_MISSING -> OpenDraftError.MissingMessageBody
        DraftOpenErrorReason.MESSAGE_DOES_NOT_EXIST -> OpenDraftError.DraftDoesNotExist
        DraftOpenErrorReason.MESSAGE_IS_NOT_A_DRAFT -> OpenDraftError.MessageIsNotADraft
        DraftOpenErrorReason.REPLY_OR_FORWARD_DRAFT -> OpenDraftError.ReplyOrForwardDraft
    }
}

fun DraftSendError.toDraftSendError(): SendDraftError = when (this) {
    is DraftSendError.Other -> SendDraftError.Other(this.v1.toDataError())
    is DraftSendError.Reason -> when (val error = this.v1) {
        is DraftSendErrorReason.AlreadySent,
        is DraftSendErrorReason.MessageAlreadySent,
        is DraftSendErrorReason.MessageIsNotADraft -> SendDraftError.AlreadySent

        is DraftSendErrorReason.AddressDisabled,
        is DraftSendErrorReason.AddressDoesNotHavePrimaryKey -> SendDraftError.InvalidSenderAddress

        is DraftSendErrorReason.RecipientEmailInvalid,
        is DraftSendErrorReason.ProtonRecipientDoesNotExist,
        is DraftSendErrorReason.NoRecipients -> SendDraftError.InvalidRecipient

        is DraftSendErrorReason.MissingAttachmentUploads -> SendDraftError.AttachmentsError
        is DraftSendErrorReason.ScheduleSendExpired,
        is DraftSendErrorReason.ScheduleSendMessageLimitExceeded -> SendDraftError.ScheduleSendError

        is DraftSendErrorReason.MessageDoesNotExist -> SendDraftError.MessageNotExisting
        is DraftSendErrorReason.PackageError -> SendDraftError.PackageError

        DraftSendErrorReason.EoPasswordDecrypt -> SendDraftError.ExternalPasswordDecryptError
        DraftSendErrorReason.ExpirationTimeTooSoon -> SendDraftError.ExpirationTimeTooSoon
        DraftSendErrorReason.MessageTooLarge -> SendDraftError.MessageIsTooLarge
        is DraftSendErrorReason.BadRequest -> SendDraftError.BadRequest(error.v1)
    }
}

fun DraftAttachmentUploadError.toObserveAttachmentsError() = when (this) {
    is DraftAttachmentUploadError.Other -> this.v1.toDataError()
    is DraftAttachmentUploadError.Reason -> when (this.v1) {
        is DraftAttachmentUploadErrorReason.MessageDoesNotExist,
        is DraftAttachmentUploadErrorReason.MessageAlreadySent,
        is DraftAttachmentUploadErrorReason.MessageDoesNotExistOnServer -> DataError.Local.NotFound

        is DraftAttachmentUploadErrorReason.Timeout -> DataError.Remote.Timeout
        is DraftAttachmentUploadErrorReason.RetryInvalidState -> DataError.Local.IllegalStateError
        is DraftAttachmentUploadErrorReason.AttachmentTooLarge,
        is DraftAttachmentUploadErrorReason.TooManyAttachments,
        is DraftAttachmentUploadErrorReason.TotalAttachmentSizeTooLarge,
        is DraftAttachmentUploadErrorReason.StorageQuotaExceeded -> DataError.Local.Unknown

        is DraftAttachmentUploadErrorReason.Crypto -> DataError.Local.CryptoError
        is DraftAttachmentUploadErrorReason.BadRequest -> DataError.Remote.BadRequest
    }
}

fun DraftAttachmentUploadError.toDeleteAttachmentError() = when (this) {
    is DraftAttachmentUploadError.Other -> AttachmentDeleteError.Other(this.v1.toDataError())
    is DraftAttachmentUploadError.Reason -> when (this.v1) {
        is DraftAttachmentUploadErrorReason.MessageDoesNotExist,
        is DraftAttachmentUploadErrorReason.MessageDoesNotExistOnServer -> AttachmentDeleteError.MessageDoesNotExist

        is DraftAttachmentUploadErrorReason.MessageAlreadySent -> AttachmentDeleteError.MessageAlreadySent
        is DraftAttachmentUploadErrorReason.Timeout,
        is DraftAttachmentUploadErrorReason.RetryInvalidState -> AttachmentDeleteError.RetriableError

        is DraftAttachmentUploadErrorReason.AttachmentTooLarge,
        is DraftAttachmentUploadErrorReason.TooManyAttachments,
        is DraftAttachmentUploadErrorReason.TotalAttachmentSizeTooLarge,
        is DraftAttachmentUploadErrorReason.Crypto,
        is DraftAttachmentUploadErrorReason.StorageQuotaExceeded ->
            AttachmentDeleteError.Other(DataError.Local.Unknown)

        is DraftAttachmentUploadErrorReason.BadRequest -> AttachmentDeleteError.Other(DataError.Remote.BadRequest)
    }
}

fun DraftPasswordError.toMessagePasswordError() = when (this) {
    is DraftPasswordError.Other -> MessagePasswordError.Other(this.v1.toDataError())
    is DraftPasswordError.Reason -> when (this.v1) {
        DraftPasswordErrorReason.PASSWORD_TOO_SHORT -> MessagePasswordError.PasswordTooShort
    }
}

fun DraftPassword?.toMessagePassword() = this?.let {
    MessagePassword(this.password, this.hint ?: "")
}

private fun LocalMimeType.toDraftMimeType() = when (this) {
    MimeType.APPLICATION_JSON,
    MimeType.APPLICATION_PDF,
    MimeType.MESSAGE_RFC822,
    MimeType.MULTIPART_MIXED,
    MimeType.MULTIPART_RELATED,
    MimeType.TEXT_HTML -> DraftMimeType.Html

    MimeType.TEXT_PLAIN -> DraftMimeType.PlainText
}

fun DraftAttachmentDispositionSwapError.toConvertError() = when (val result = this) {
    is DraftAttachmentDispositionSwapError.Other -> ConvertAttachmentError.Other(result.v1.toDataError())
    is DraftAttachmentDispositionSwapError.Reason -> result.toConvertAttachmentError()
}

