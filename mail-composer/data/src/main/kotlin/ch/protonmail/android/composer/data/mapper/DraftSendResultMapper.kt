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

package ch.protonmail.android.composer.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalDraftSendResult
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError.AddressDisabled
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError.AddressDoesNotHavePrimaryKey
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError.InvalidRecipient
import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import uniffi.proton_mail_uniffi.DraftAttachmentUploadErrorReason
import uniffi.proton_mail_uniffi.DraftSaveError
import uniffi.proton_mail_uniffi.DraftSaveErrorReason
import uniffi.proton_mail_uniffi.DraftSendErrorReason
import uniffi.proton_mail_uniffi.DraftSendFailure
import uniffi.proton_mail_uniffi.DraftSendResultOrigin
import uniffi.proton_mail_uniffi.DraftSendStatus
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

fun LocalDraftSendResult.toMessageSendingStatus(): MessageSendingStatus = when (val status = this.error) {
    is DraftSendStatus.Success -> this.toMessageSendingStatusForSuccess(status)
    is DraftSendStatus.Failure -> this.toMessageSendingStatusForFailure(status.v1)
}

private fun LocalDraftSendResult.toMessageSendingStatusForSuccess(
    status: DraftSendStatus.Success
): MessageSendingStatus = when (this.origin) {
    DraftSendResultOrigin.SAVE,
    DraftSendResultOrigin.SAVE_BEFORE_SEND,
    DraftSendResultOrigin.ATTACHMENT_UPLOAD -> MessageSendingStatus.NoStatus(this.messageId.toMessageId())

    DraftSendResultOrigin.SEND -> {
        val timeRemainingForUndo = status.secondsUntilCancel.toInt()
        if (timeRemainingForUndo > 0) {
            MessageSendingStatus.MessageSentUndoable(
                messageId = this.messageId.toMessageId(),
                timeRemainingForUndo = timeRemainingForUndo.toDuration(DurationUnit.SECONDS)
            )
        } else {
            MessageSendingStatus.MessageSentFinal(this.messageId.toMessageId())
        }
    }

    DraftSendResultOrigin.SCHEDULE_SEND -> {
        val timeRemainingForUndo = status.secondsUntilCancel.toInt()
        val deliveryTime = status.deliveryTime.toLong()
        if (timeRemainingForUndo > 0) {
            MessageSendingStatus.MessageScheduledUndoable(
                messageId = this.messageId.toMessageId(),
                deliveryTime = Instant.fromEpochSeconds(deliveryTime)
            )
        } else {
            MessageSendingStatus.MessageSentFinal(this.messageId.toMessageId())
        }
    }
}

private fun LocalDraftSendResult.toMessageSendingStatusForFailure(error: DraftSendFailure): MessageSendingStatus {
    return when (error) {
        is DraftSendFailure.AttachmentUpload -> MessageSendingStatus.SendMessageError(
            messageId = this.messageId.toMessageId(),
            reason = error.v1.toSendErrorReason()
        )

        is DraftSendFailure.Other -> MessageSendingStatus.SendMessageError(
            messageId = this.messageId.toMessageId(),
            reason = SendErrorReason.OtherDataError(error.v1.toDataError())
        )

        is DraftSendFailure.Save -> MessageSendingStatus.SendMessageError(
            messageId = this.messageId.toMessageId(),
            reason = error.v1.toSendErrorReason()
        )

        is DraftSendFailure.Send -> MessageSendingStatus.SendMessageError(
            messageId = this.messageId.toMessageId(),
            reason = error.v1.toSendErrorReason()
        )
    }
}

fun DraftSaveErrorReason.toSendErrorReason(): SendErrorReason = when (this) {
    is DraftSaveErrorReason.MessageAlreadySent,
    is DraftSaveErrorReason.MessageIsNotADraft -> SendErrorReason.ErrorNoMessage.AlreadySent

    is DraftSaveErrorReason.AddressDisabled ->
        SendErrorReason.ErrorWithMessage.AddressDisabled(v1)

    is DraftSaveErrorReason.AddressDoesNotHavePrimaryKey ->
        SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey(v1)

    is DraftSaveErrorReason.RecipientEmailInvalid -> SendErrorReason.ErrorWithMessage.RecipientEmailInvalid(v1)

    is DraftSaveErrorReason.ProtonRecipientDoesNotExist ->
        SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist(v1)

    is DraftSaveErrorReason.MessageDoesNotExist -> SendErrorReason.ErrorNoMessage.MessageDoesNotExist
    DraftSaveErrorReason.AttachmentTooLarge -> SendErrorReason.ErrorNoMessage.AttachmentTooLarge
    DraftSaveErrorReason.TooManyAttachments -> SendErrorReason.ErrorNoMessage.TooManyAttachments
    DraftSaveErrorReason.TotalAttachmentSizeTooLarge -> SendErrorReason.ErrorNoMessage.AttachmentTooLarge
}

fun DraftAttachmentUploadErrorReason.toSendErrorReason(): SendErrorReason = when (this) {
    DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST,
    DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST_ON_SERVER,
    DraftAttachmentUploadErrorReason.MESSAGE_ALREADY_SENT -> SendErrorReason.ErrorNoMessage.AlreadySent

    DraftAttachmentUploadErrorReason.CRYPTO -> SendErrorReason.ErrorNoMessage.AttachmentCryptoFailure
    DraftAttachmentUploadErrorReason.ATTACHMENT_TOO_LARGE -> SendErrorReason.ErrorNoMessage.AttachmentTooLarge
    DraftAttachmentUploadErrorReason.TOO_MANY_ATTACHMENTS -> SendErrorReason.ErrorNoMessage.TooManyAttachments

    DraftAttachmentUploadErrorReason.TIMEOUT,
    DraftAttachmentUploadErrorReason.RETRY_INVALID_STATE ->
        SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable

    DraftAttachmentUploadErrorReason.TOTAL_ATTACHMENT_SIZE_TOO_LARGE ->
        SendErrorReason.ErrorNoMessage.AttachmentTooLarge
}

fun DraftSendErrorReason.toSendErrorReason(): SendErrorReason = when (this) {
    DraftSendErrorReason.NoRecipients -> SendErrorReason.ErrorNoMessage.NoRecipients
    DraftSendErrorReason.AlreadySent -> SendErrorReason.ErrorNoMessage.AlreadySent
    DraftSendErrorReason.MessageDoesNotExist -> SendErrorReason.ErrorNoMessage.MessageDoesNotExist
    DraftSendErrorReason.MessageIsNotADraft -> SendErrorReason.ErrorNoMessage.MessageIsNotADraft
    DraftSendErrorReason.MessageAlreadySent -> SendErrorReason.ErrorNoMessage.MessageAlreadySent
    DraftSendErrorReason.MissingAttachmentUploads -> SendErrorReason.ErrorNoMessage.MissingAttachmentUploads
    DraftSendErrorReason.ScheduleSendMessageLimitExceeded -> SendErrorReason.ErrorNoMessage.ScheduledSendMessagesLimit
    DraftSendErrorReason.ScheduleSendExpired -> SendErrorReason.ErrorNoMessage.ScheduledSendExpired
    DraftSendErrorReason.ExpirationTimeTooSoon -> SendErrorReason.ErrorNoMessage.ExpirationTimeTooSoon

    is DraftSendErrorReason.AddressDoesNotHavePrimaryKey ->
        SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey(v1)

    is DraftSendErrorReason.RecipientEmailInvalid ->
        SendErrorReason.ErrorWithMessage.RecipientEmailInvalid(v1)

    is DraftSendErrorReason.ProtonRecipientDoesNotExist ->
        SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist(v1)

    is DraftSendErrorReason.AddressDisabled ->
        SendErrorReason.ErrorWithMessage.AddressDisabled(v1)

    is DraftSendErrorReason.PackageError ->
        SendErrorReason.ErrorWithMessage.PackageError(v1)

    is DraftSendErrorReason.EoPasswordDecrypt -> SendErrorReason.ErrorNoMessage.ExternalPasswordDecryptFailed
}

fun DraftSaveError.toSaveDraftError(): SaveDraftError = when (this) {
    is DraftSaveError.Other -> SaveDraftError.Other(this.v1.toDataError())
    is DraftSaveError.Reason -> when (val reason = this.v1) {
        is DraftSaveErrorReason.MessageAlreadySent,
        is DraftSaveErrorReason.MessageDoesNotExist,
        is DraftSaveErrorReason.MessageIsNotADraft -> SaveDraftError.MessageIsNotADraft

        is DraftSaveErrorReason.AddressDisabled -> AddressDisabled(reason.v1)
        is DraftSaveErrorReason.AddressDoesNotHavePrimaryKey -> AddressDoesNotHavePrimaryKey(reason.v1)
        is DraftSaveErrorReason.RecipientEmailInvalid -> InvalidRecipient(reason.v1)
        is DraftSaveErrorReason.ProtonRecipientDoesNotExist -> InvalidRecipient(reason.v1)
        is DraftSaveErrorReason.AttachmentTooLarge -> SaveDraftError.AttachmentsTooLarge
        is DraftSaveErrorReason.TooManyAttachments -> SaveDraftError.TooManyAttachments
        is DraftSaveErrorReason.TotalAttachmentSizeTooLarge -> SaveDraftError.AttachmentsTooLarge
    }
}
