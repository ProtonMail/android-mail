/*
 * Copyright (c) 2026 Proton Technologies AG
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
import uniffi.mail_uniffi.DraftSaveErrorReason
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class DraftSaveErrorReasonMapperTest(
    @Suppress("unused") private val testName: String,
    private val error: DraftSaveErrorReason,
    private val expected: SendErrorReason
) {

    @Test
    fun `should map draft save error reason correctly`() {
        val actual = error.toSendErrorReason()
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "MessageAlreadySent maps to AlreadySent",
                DraftSaveErrorReason.MessageAlreadySent,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MessageIsNotADraft maps to AlreadySent",
                DraftSaveErrorReason.MessageIsNotADraft,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "AddressDisabled maps with details",
                DraftSaveErrorReason.AddressDisabled("address"),
                SendErrorReason.ErrorWithMessage.AddressDisabled("address")
            ),
            arrayOf(
                "AddressDoesNotHavePrimaryKey maps with details",
                DraftSaveErrorReason.AddressDoesNotHavePrimaryKey("address"),
                SendErrorReason.ErrorWithMessage.AddressDoesNotHavePrimaryKey("address")
            ),
            arrayOf(
                "RecipientEmailInvalid maps with details",
                DraftSaveErrorReason.RecipientEmailInvalid("email"),
                SendErrorReason.ErrorWithMessage.RecipientEmailInvalid("email")
            ),
            arrayOf(
                "ProtonRecipientDoesNotExist maps with details",
                DraftSaveErrorReason.ProtonRecipientDoesNotExist("email"),
                SendErrorReason.ErrorWithMessage.ProtonRecipientDoesNotExist("email")
            ),
            arrayOf(
                "MessageDoesNotExist maps to MessageDoesNotExist",
                DraftSaveErrorReason.MessageDoesNotExist,
                SendErrorReason.ErrorNoMessage.MessageDoesNotExist
            ),
            arrayOf(
                "AttachmentTooLarge maps to AttachmentTooLarge",
                DraftSaveErrorReason.AttachmentTooLarge,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            ),
            arrayOf(
                "TooManyAttachments maps to TooManyAttachments",
                DraftSaveErrorReason.TooManyAttachments,
                SendErrorReason.ErrorNoMessage.TooManyAttachments
            ),
            arrayOf(
                "TotalAttachmentSizeTooLarge maps to AttachmentTooLarge",
                DraftSaveErrorReason.TotalAttachmentSizeTooLarge,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            )
        )
    }
}
