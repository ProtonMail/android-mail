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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.QuotedHtmlContent
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.AttachmentGroupUiModel

data class ComposerDraftState(
    val fields: ComposerFields,
    val attachments: AttachmentGroupUiModel,
    val premiumFeatureMessage: Effect<TextUiModel>,
    val error: Effect<TextUiModel>,
    val isSubmittable: Boolean,
    val senderAddresses: List<SenderUiModel>,
    val changeBottomSheetVisibility: Effect<Boolean>,
    val closeComposer: Effect<Unit>,
    val closeComposerWithDraftSaved: Effect<Unit>,
    val closeComposerWithMessageSending: Effect<Unit>,
    val closeComposerWithMessageSendingOffline: Effect<Unit>,
    val isLoading: Boolean,
    val isAddAttachmentsButtonVisible: Boolean,
    val attachmentsFileSizeExceeded: Effect<Unit>
) {

    companion object {

        fun initial(
            draftId: MessageId,
            to: List<RecipientUiModel> = emptyList(),
            cc: List<RecipientUiModel> = emptyList(),
            bcc: List<RecipientUiModel> = emptyList(),
            isSubmittable: Boolean = false
        ): ComposerDraftState = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = SenderUiModel(""),
                to = to,
                cc = cc,
                bcc = bcc,
                subject = "",
                body = ""
            ),
            attachments = AttachmentGroupUiModel(
                attachments = emptyList()
            ),
            premiumFeatureMessage = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = isSubmittable,
            senderAddresses = emptyList(),
            changeBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            closeComposerWithMessageSending = Effect.empty(),
            closeComposerWithMessageSendingOffline = Effect.empty(),
            isLoading = false,
            isAddAttachmentsButtonVisible = false,
            attachmentsFileSizeExceeded = Effect.empty()
        )
    }
}

data class ComposerFields(
    val draftId: MessageId,
    val sender: SenderUiModel,
    val to: List<RecipientUiModel>,
    val cc: List<RecipientUiModel>,
    val bcc: List<RecipientUiModel>,
    val subject: String,
    val body: String,
    val quotedBody: QuotedHtmlContent? = null
)
