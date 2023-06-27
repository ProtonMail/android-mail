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
import ch.protonmail.android.mailmessage.domain.entity.MessageId

sealed class ComposerDraftState(
    open val fields: ComposerFields,
    open val premiumFeatureMessage: Effect<TextUiModel>,
    open val error: Effect<TextUiModel>
) {

    data class Submittable(
        override val fields: ComposerFields,
        override val premiumFeatureMessage: Effect<TextUiModel>,
        override val error: Effect<TextUiModel>
    ) : ComposerDraftState(fields, premiumFeatureMessage, error)

    data class NotSubmittable(
        override val fields: ComposerFields,
        override val premiumFeatureMessage: Effect<TextUiModel>,
        override val error: Effect<TextUiModel>
    ) : ComposerDraftState(fields, premiumFeatureMessage, error)

    companion object {

        fun empty(draftId: MessageId): ComposerDraftState = NotSubmittable(
            fields = ComposerFields(
                draftId = draftId,
                from = "",
                to = emptyList(),
                cc = emptyList(),
                bcc = emptyList(),
                subject = "",
                body = ""
            ),
            premiumFeatureMessage = Effect.empty(),
            error = Effect.empty()
        )
    }
}

data class ComposerFields(
    val draftId: MessageId,
    val from: String,
    val to: List<RecipientUiModel>,
    val cc: List<RecipientUiModel>,
    val bcc: List<RecipientUiModel>,
    val subject: String,
    val body: String
)
