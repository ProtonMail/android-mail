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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.presentation.R
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class SaveDraftErrorMapperTest(
    @Suppress("unused") private val testName: String,
    val error: SaveDraftError,
    val expected: TextUiModel
) {


    @Test
    fun `should reduce the state correctly`() {
        val actual = SaveDraftErrorMapper.toTextUiModel(error)
        assertEquals(expected, actual)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "address disabled",
                SaveDraftError.AddressDisabled("address"),
                TextUiModel(R.string.composer_error_store_draft_address_disabled)
            ),
            arrayOf(
                "address has no primary key",
                SaveDraftError.AddressDoesNotHavePrimaryKey("address"),
                TextUiModel(R.string.composer_error_store_draft_address_pk_missing)
            ),
            arrayOf(
                "attachments too large",
                SaveDraftError.AttachmentsTooLarge,
                TextUiModel(R.string.composer_error_store_draft_attachment_too_large)
            ),
            arrayOf(
                "duplicate recipient",
                SaveDraftError.DuplicateRecipient,
                TextUiModel(R.string.composer_error_store_draft_duplicate_recipient)
            ),
            arrayOf(
                "empty recipient group name",
                SaveDraftError.EmptyRecipientGroupName,
                TextUiModel(R.string.composer_error_store_draft_recipient_group_name)
            ),
            arrayOf(
                "invalid recipient",
                SaveDraftError.InvalidRecipient("message"),
                TextUiModel(R.string.composer_error_store_draft_invalid_recipient)
            ),
            arrayOf(
                "message is not a draft",
                SaveDraftError.MessageIsNotADraft,
                TextUiModel(R.string.composer_error_store_draft_message_not_a_draft)
            ),
            arrayOf(
                "too many attachments",
                SaveDraftError.TooManyAttachments,
                TextUiModel(R.string.composer_error_store_draft_too_many_attachments)
            ),
            arrayOf(
                "bad request",
                SaveDraftError.BadRequest("Recipient address is invalid"),
                TextUiModel("Recipient address is invalid")
            )
        )
    }
}
