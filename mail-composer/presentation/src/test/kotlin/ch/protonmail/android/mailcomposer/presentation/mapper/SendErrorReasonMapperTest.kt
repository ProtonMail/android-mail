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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import ch.protonmail.android.mailcomposer.presentation.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class SendErrorReasonMapperTest(
    @Suppress("unused") private val testName: String,
    private val error: SendErrorReason,
    private val expectedResIdOrString: Any
) {

    private val context = mockk<Context>()

    @Test
    fun `should map send error reason correctly`() {
        // Given
        val expectedString = "resolved_string"
        if (expectedResIdOrString is Int) {
            every { context.getString(expectedResIdOrString) } returns expectedString
        }

        // When
        val actual = SendErrorReasonMapper.toSendErrorMessage(context, error)

        // Then
        if (expectedResIdOrString is Int) {
            verify { context.getString(expectedResIdOrString) }
            assertEquals(expectedString, actual)
        } else {
            assertEquals(expectedResIdOrString, actual)
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "MessageAlreadySent returns already sent error",
                SendErrorReason.ErrorNoMessage.MessageAlreadySent,
                R.string.composer_error_send_draft_already_sent
            ),
            arrayOf(
                "AlreadySent returns already sent error",
                SendErrorReason.ErrorNoMessage.AlreadySent,
                R.string.composer_error_send_draft_already_sent
            ),
            arrayOf(
                "AttachmentCryptoFailure returns crypto error",
                SendErrorReason.ErrorNoMessage.AttachmentCryptoFailure,
                R.string.composer_error_send_draft_attachments_crypto_error
            ),
            arrayOf(
                "AttachmentTooLarge returns attachment too large error",
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge,
                R.string.composer_error_send_draft_attachment_too_large
            ),
            arrayOf(
                "MissingAttachmentUploads returns attachments error",
                SendErrorReason.ErrorNoMessage.MissingAttachmentUploads,
                R.string.composer_error_send_draft_attachments_error
            ),
            arrayOf(
                "AttachmentUploadFailureRetriable returns attachments error",
                SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable,
                R.string.composer_error_send_draft_attachments_error
            ),
            arrayOf(
                "AttachmentConversionFailure returns conversion error",
                SendErrorReason.ErrorNoMessage.AttachmentConversionFailure,
                R.string.composer_error_send_draft_attachments_conversion_error
            ),
            arrayOf(
                "ExpirationTimeTooSoon returns expiration time error",
                SendErrorReason.ErrorNoMessage.ExpirationTimeTooSoon,
                R.string.composer_error_send_draft_expiration_time_too_soon
            ),
            arrayOf(
                "ExternalPasswordDecryptFailed returns password decrypt error",
                SendErrorReason.ErrorNoMessage.ExternalPasswordDecryptFailed,
                R.string.composer_error_send_draft_external_password_decrypt_error
            ),
            arrayOf(
                "MessageIsNotADraft returns message does not exist error",
                SendErrorReason.ErrorNoMessage.MessageIsNotADraft,
                R.string.composer_error_send_draft_message_does_not_exist
            ),
            arrayOf(
                "MessageDoesNotExist returns message does not exist error",
                SendErrorReason.ErrorNoMessage.MessageDoesNotExist,
                R.string.composer_error_send_draft_message_does_not_exist
            ),
            arrayOf(
                "MessageTooLarge returns message too large error",
                SendErrorReason.ErrorNoMessage.MessageTooLarge,
                R.string.composer_error_send_draft_message_too_large
            ),
            arrayOf(
                "ScheduledSendExpired returns schedule send error",
                SendErrorReason.ErrorNoMessage.ScheduledSendExpired,
                R.string.composer_error_send_draft_schedule_send_error
            ),
            arrayOf(
                "ScheduledSendMessagesLimit returns schedule send error",
                SendErrorReason.ErrorNoMessage.ScheduledSendMessagesLimit,
                R.string.composer_error_send_draft_schedule_send_error
            ),
            arrayOf(
                "TooManyAttachments returns too many attachments error",
                SendErrorReason.ErrorNoMessage.TooManyAttachments,
                R.string.composer_error_send_draft_too_many_attachments
            ),
            arrayOf(
                "AddressDisabled returns invalid sender error",
                SendErrorReason.ErrorWithMessage.AddressDisabled("address"),
                R.string.composer_error_send_draft_invalid_sender
            ),
            arrayOf(
                "AddressDoesNotHavePrimaryKey returns invalid sender error",
                SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey("address"),
                R.string.composer_error_send_draft_invalid_sender
            ),
            arrayOf(
                "PackageError returns package error",
                SendErrorReason.ErrorWithMessage.PackageError("details"),
                R.string.composer_error_send_draft_package_error
            ),
            arrayOf(
                "NoRecipients returns invalid recipient error",
                SendErrorReason.ErrorNoMessage.NoRecipients,
                R.string.composer_error_send_draft_invalid_recipient
            ),
            arrayOf(
                "ProtonRecipientDoesNotExist returns invalid recipient error",
                SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist("email"),
                R.string.composer_error_send_draft_invalid_recipient
            ),
            arrayOf(
                "RecipientEmailInvalid returns invalid recipient error",
                SendErrorReason.ErrorWithMessage.RecipientEmailInvalid("email"),
                R.string.composer_error_send_draft_invalid_recipient
            ),
            arrayOf(
                "StorageQuotaExceeded returns storage quota error",
                SendErrorReason.ErrorNoMessage.StorageQuotaExceeded,
                R.string.composer_storage_quota_exceeded_error
            ),
            arrayOf(
                "OtherDataError returns generic error",
                SendErrorReason.OtherDataError(DataError.Local.Unknown),
                R.string.composer_error_send_draft_generic
            ),
            arrayOf(
                "AttachmentRemove returns generic error",
                SendErrorReason.ErrorNoMessage.AttachmentRemove,
                R.string.composer_error_send_draft_generic
            ),
            arrayOf(
                "BadRequest returns the details string directly",
                SendErrorReason.ErrorWithMessage.BadRequest("Server error details"),
                "Server error details"
            )
        )
    }
}
