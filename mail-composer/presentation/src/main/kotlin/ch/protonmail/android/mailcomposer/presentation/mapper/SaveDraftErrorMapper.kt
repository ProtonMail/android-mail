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

internal object SaveDraftErrorMapper {

    fun toTextUiModel(saveDraftError: SaveDraftError): TextUiModel = when (saveDraftError) {
        is SaveDraftError.AddressDisabled ->
            TextUiModel(R.string.composer_error_store_draft_address_disabled)
        is SaveDraftError.AddressDoesNotHavePrimaryKey ->
            TextUiModel(R.string.composer_error_store_draft_address_pk_missing)
        is SaveDraftError.AttachmentsTooLarge ->
            TextUiModel(R.string.composer_error_store_draft_attachment_too_large)
        is SaveDraftError.BadRequest ->
            TextUiModel(saveDraftError.message)
        is SaveDraftError.DuplicateRecipient ->
            TextUiModel(R.string.composer_error_store_draft_duplicate_recipient)
        is SaveDraftError.EmptyRecipientGroupName ->
            TextUiModel(R.string.composer_error_store_draft_recipient_group_name)
        is SaveDraftError.InvalidRecipient ->
            TextUiModel(R.string.composer_error_store_draft_invalid_recipient)
        is SaveDraftError.MessageIsNotADraft ->
            TextUiModel(R.string.composer_error_store_draft_message_not_a_draft)
        is SaveDraftError.TooManyAttachments ->
            TextUiModel(R.string.composer_error_store_draft_too_many_attachments)
        is SaveDraftError.Other,
        SaveDraftError.SaveFailed ->
            TextUiModel(R.string.composer_error_store_draft_generic)
    }
}
