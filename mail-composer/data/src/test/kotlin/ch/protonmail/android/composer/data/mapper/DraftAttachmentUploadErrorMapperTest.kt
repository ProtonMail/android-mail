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
import uniffi.mail_uniffi.DraftAttachmentUploadErrorReason
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class DraftAttachmentUploadErrorReasonMapperTest(
    @Suppress("unused") private val testName: String,
    private val error: DraftAttachmentUploadErrorReason,
    private val expected: SendErrorReason
) {

    @Test
    fun `should map draft attachment upload error reason correctly`() {
        val actual = error.toSendErrorReason()
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "MessageDoesNotExist maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MessageDoesNotExist,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MessageDoesNotExistOnServer maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MessageDoesNotExistOnServer,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MessageAlreadySent maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MessageAlreadySent,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "Crypto maps to AttachmentCryptoFailure",
                DraftAttachmentUploadErrorReason.Crypto,
                SendErrorReason.ErrorNoMessage.AttachmentCryptoFailure
            ),
            arrayOf(
                "AttachmentTooLarge maps to AttachmentTooLarge",
                DraftAttachmentUploadErrorReason.AttachmentTooLarge,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            ),
            arrayOf(
                "TooManyAttachments maps to TooManyAttachments",
                DraftAttachmentUploadErrorReason.TooManyAttachments,
                SendErrorReason.ErrorNoMessage.TooManyAttachments
            ),
            arrayOf(
                "Timeout maps to AttachmentUploadFailureRetriable",
                DraftAttachmentUploadErrorReason.Timeout,
                SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable
            ),
            arrayOf(
                "RetryInvalidState maps to AttachmentUploadFailureRetriable",
                DraftAttachmentUploadErrorReason.RetryInvalidState,
                SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable
            ),
            arrayOf(
                "TotalAttachmentSizeTooLarge maps to AttachmentTooLarge",
                DraftAttachmentUploadErrorReason.TotalAttachmentSizeTooLarge,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            ),
            arrayOf(
                "StorageQuotaExceeded maps to StorageQuotaExceeded",
                DraftAttachmentUploadErrorReason.StorageQuotaExceeded,
                SendErrorReason.ErrorNoMessage.StorageQuotaExceeded
            )
        )
    }
}
