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
                "MESSAGE_DOES_NOT_EXIST maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MESSAGE_DOES_NOT_EXIST_ON_SERVER maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MESSAGE_DOES_NOT_EXIST_ON_SERVER,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "MESSAGE_ALREADY_SENT maps to AlreadySent",
                DraftAttachmentUploadErrorReason.MESSAGE_ALREADY_SENT,
                SendErrorReason.ErrorNoMessage.AlreadySent
            ),
            arrayOf(
                "CRYPTO maps to AttachmentCryptoFailure",
                DraftAttachmentUploadErrorReason.CRYPTO,
                SendErrorReason.ErrorNoMessage.AttachmentCryptoFailure
            ),
            arrayOf(
                "ATTACHMENT_TOO_LARGE maps to AttachmentTooLarge",
                DraftAttachmentUploadErrorReason.ATTACHMENT_TOO_LARGE,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            ),
            arrayOf(
                "TOO_MANY_ATTACHMENTS maps to TooManyAttachments",
                DraftAttachmentUploadErrorReason.TOO_MANY_ATTACHMENTS,
                SendErrorReason.ErrorNoMessage.TooManyAttachments
            ),
            arrayOf(
                "TIMEOUT maps to AttachmentUploadFailureRetriable",
                DraftAttachmentUploadErrorReason.TIMEOUT,
                SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable
            ),
            arrayOf(
                "RETRY_INVALID_STATE maps to AttachmentUploadFailureRetriable",
                DraftAttachmentUploadErrorReason.RETRY_INVALID_STATE,
                SendErrorReason.ErrorNoMessage.AttachmentUploadFailureRetriable
            ),
            arrayOf(
                "TOTAL_ATTACHMENT_SIZE_TOO_LARGE maps to AttachmentTooLarge",
                DraftAttachmentUploadErrorReason.TOTAL_ATTACHMENT_SIZE_TOO_LARGE,
                SendErrorReason.ErrorNoMessage.AttachmentTooLarge
            ),
            arrayOf(
                "STORAGE_QUOTA_EXCEEDED maps to StorageQuotaExceeded",
                DraftAttachmentUploadErrorReason.STORAGE_QUOTA_EXCEEDED,
                SendErrorReason.ErrorNoMessage.StorageQuotaExceeded
            )
        )
    }
}
