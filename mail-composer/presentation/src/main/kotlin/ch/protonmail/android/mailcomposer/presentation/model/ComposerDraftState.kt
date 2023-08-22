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
import ch.protonmail.android.mailmessage.domain.model.MessageId

data class ComposerDraftState(
    val fields: ComposerFields,
    val premiumFeatureMessage: Effect<TextUiModel>,
    val error: Effect<TextUiModel>,
    val isSubmittable: Boolean,
    val senderAddresses: List<SenderUiModel>,
    val changeSenderBottomSheetVisibility: Effect<Boolean>,
    val closeComposer: Effect<Unit>,
    val closeComposerWithDraftSaved: Effect<Unit>,
    val isLoading: Boolean,
    val isAddAttachmentsButtonVisible: Boolean
) {

    companion object {

        fun initial(draftId: MessageId): ComposerDraftState = ComposerDraftState(
            fields = ComposerFields(
                draftId = draftId,
                sender = SenderUiModel(""),
                to = emptyList(),
                cc = emptyList(),
                bcc = emptyList(),
                subject = "",
                body = ""
            ),
            premiumFeatureMessage = Effect.empty(),
            error = Effect.empty(),
            isSubmittable = false,
            senderAddresses = emptyList(),
            changeSenderBottomSheetVisibility = Effect.empty(),
            closeComposer = Effect.empty(),
            closeComposerWithDraftSaved = Effect.empty(),
            isLoading = false,
            isAddAttachmentsButtonVisible = false
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
    val body: String
)
