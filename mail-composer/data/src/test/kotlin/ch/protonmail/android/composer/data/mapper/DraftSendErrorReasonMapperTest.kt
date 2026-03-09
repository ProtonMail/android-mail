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

import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uniffi.mail_uniffi.DraftSendErrorReason
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class DraftSendErrorReasonMapperTest(
    @Suppress("unused") private val testName: String,
    private val error: DraftSendErrorReason,
    private val expected: SendErrorReason
) {

    @Test
    fun `should map draft send error reason correctly`() {
        val actual = error.toSendErrorReason()
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "NoRecipients maps to NoRecipients",
                DraftSendErrorReason.NoRecipients,
                SendErrorReason.ErrorNoMessage.NoRecipients
            ),
            arrayOf(
                "AlreadySent maps to AlreadySent",
                DraftSendErrorReason.AlreadySent,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MessageDoesNotExist maps to MessageDoesNotExist",
                DraftSendErrorReason.MessageDoesNotExist,
                SendErrorReason.ErrorNoMessage.MessageDoesNotExist
            ),
            arrayOf(
                "MessageIsNotADraft maps to MessageIsNotADraft",
                DraftSendErrorReason.MessageIsNotADraft,
                SendErrorReason.ErrorNoMessage.MessageIsNotADraft
            ),
            arrayOf(
                "MessageAlreadySent maps to MessageAlreadySent",
                DraftSendErrorReason.MessageAlreadySent,
                SendErrorReason.ErrorNoMessage.MessageAlreadySent
            ),
            arrayOf(
                "MissingAttachmentUploads maps to MissingAttachmentUploads",
                DraftSendErrorReason.MissingAttachmentUploads,
                SendErrorReason.ErrorNoMessage.MissingAttachmentUploads
            ),
            arrayOf(
                "ScheduleSendMessageLimitExceeded maps to ScheduledSendMessagesLimit",
                DraftSendErrorReason.ScheduleSendMessageLimitExceeded,
                SendErrorReason.ErrorNoMessage.ScheduledSendMessagesLimit
            ),
            arrayOf(
                "ScheduleSendExpired maps to ScheduledSendExpired",
                DraftSendErrorReason.ScheduleSendExpired,
                SendErrorReason.ErrorNoMessage.ScheduledSendExpired
            ),
            arrayOf(
                "ExpirationTimeTooSoon maps to ExpirationTimeTooSoon",
                DraftSendErrorReason.ExpirationTimeTooSoon,
                SendErrorReason.ErrorNoMessage.ExpirationTimeTooSoon
            ),
            arrayOf(
                "AddressDoesNotHavePrimaryKey maps with details",
                DraftSendErrorReason.AddressDoesNotHavePrimaryKey("address"),
                SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey("address")
            ),
            arrayOf(
                "RecipientEmailInvalid maps with details",
                DraftSendErrorReason.RecipientEmailInvalid("email@test.com"),
                SendErrorReason.ErrorWithMessage.RecipientEmailInvalid("email@test.com")
            ),
            arrayOf(
                "ProtonRecipientDoesNotExist maps with details",
                DraftSendErrorReason.ProtonRecipientDoesNotExist("user@proton.me"),
                SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist("user@proton.me")
            ),
            arrayOf(
                "AddressDisabled maps with details",
                DraftSendErrorReason.AddressDisabled("disabled@test.com"),
                SendErrorReason.ErrorWithMessage.AddressDisabled("disabled@test.com")
            ),
            arrayOf(
                "PackageError maps with details",
                DraftSendErrorReason.PackageError("package error details"),
                SendErrorReason.ErrorWithMessage.PackageError("package error details")
            ),
            arrayOf(
                "EoPasswordDecrypt maps to ExternalPasswordDecryptFailed",
                DraftSendErrorReason.EoPasswordDecrypt,
                SendErrorReason.ErrorNoMessage.ExternalPasswordDecryptFailed
            ),
            arrayOf(
                "MessageTooLarge maps to MessageTooLarge",
                DraftSendErrorReason.MessageTooLarge,
                SendErrorReason.ErrorNoMessage.MessageTooLarge
            ),
            arrayOf(
                "BadRequest maps with details",
                DraftSendErrorReason.BadRequest("bad request details"),
                SendErrorReason.ErrorWithMessage.BadRequest("bad request details")
            )
        )
    }
}
