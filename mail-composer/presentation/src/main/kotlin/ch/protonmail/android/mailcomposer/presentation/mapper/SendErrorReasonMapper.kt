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

package ch.protonmail.android.mailcomposer.presentation.mapper

import android.content.Context
import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import ch.protonmail.android.mailcomposer.presentation.R

object SendErrorReasonMapper {

    fun toSendErrorMessage(context: Context, sendDraftError: SendErrorReason): String = when (sendDraftError) {
        SendErrorReason.ErrorNoMessage.MessageAlreadySent,
        SendErrorReason.ErrorNoMessage.AlreadySent ->
            context.getString(R.string.composer_error_send_draft_already_sent)

        SendErrorReason.ErrorNoMessage.AttachmentCryptoFailure ->
            context.getString(R.string.composer_error_send_draft_attachments_crypto_error)

        SendErrorReason.ErrorNoMessage.AttachmentTooLarge ->
            context.getString(R.string.composer_error_send_draft_attachment_too_large)

        SendErrorReason.ErrorNoMessage.MissingAttachmentUploads,
        SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable ->
            context.getString(R.string.composer_error_send_draft_attachments_error)

        SendErrorReason.ErrorNoMessage.AttachmentConversionFailure ->
            context.getString(R.string.composer_error_send_draft_attachments_conversion_error)

        SendErrorReason.ErrorNoMessage.ExpirationTimeTooSoon ->
            context.getString(R.string.composer_error_send_draft_expiration_time_too_soon)

        SendErrorReason.ErrorNoMessage.ExternalPasswordDecryptFailed ->
            context.getString(R.string.composer_error_send_draft_external_password_decrypt_error)

        SendErrorReason.ErrorNoMessage.MessageIsNotADraft,
        SendErrorReason.ErrorNoMessage.MessageDoesNotExist ->
            context.getString(R.string.composer_error_send_draft_message_does_not_exist)

        SendErrorReason.ErrorNoMessage.MessageTooLarge ->
            context.getString(R.string.composer_error_send_draft_message_too_large)

        SendErrorReason.ErrorNoMessage.ScheduledSendExpired,
        SendErrorReason.ErrorNoMessage.ScheduledSendMessagesLimit ->
            context.getString(R.string.composer_error_send_draft_schedule_send_error)

        SendErrorReason.ErrorNoMessage.TooManyAttachments ->
            context.getString(R.string.composer_error_send_draft_too_many_attachments)

        is SendErrorReason.ErrorWithMessage.AddressDisabled,
        is SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey ->
            context.getString(R.string.composer_error_send_draft_invalid_sender)

        is SendErrorReason.ErrorWithMessage.PackageError ->
            context.getString(R.string.composer_error_send_draft_package_error)

        SendErrorReason.ErrorNoMessage.NoRecipients,
        is SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist,
        is SendErrorReason.ErrorWithMessage.RecipientEmailInvalid ->
            context.getString(R.string.composer_error_send_draft_invalid_recipient)

        is SendErrorReason.ErrorNoMessage.StorageQuotaExceeded ->
            context.getString(R.string.composer_storage_quota_exceeded_error)

        is SendErrorReason.ErrorNoMessage.AttachmentRemove,
        is SendErrorReason.OtherDataError -> context.getString(R.string.composer_error_send_draft_generic)
        is SendErrorReason.ErrorWithMessage.BadRequest -> sendDraftError.details
    }
}
