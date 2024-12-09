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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.raise.either
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithAllFields @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val prepareAndEncryptDraftBody: PrepareAndEncryptDraftBody,
    private val saveDraft: SaveDraft,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        draftMessageId: MessageId,
        fields: DraftFields,
        action: DraftAction = DraftAction.Compose
    ) = either {
        transactor.performTransaction {
            val draftWithBody = prepareAndEncryptDraftBody(
                userId, draftMessageId, fields.body, fields.originalHtmlQuote, fields.sender
            ).bind()

            val updatedDraft = draftWithBody.copy(
                message = draftWithBody.message.copy(
                    subject = fields.subject.value,
                    toList = fields.recipientsTo.value,
                    ccList = fields.recipientsCc.value,
                    bccList = fields.recipientsBcc.value
                )
            )
            saveDraft(updatedDraft, userId)

            draftStateRepository.createOrUpdateLocalState(userId, draftMessageId, action)
        }
        Timber.d("Draft: finished storing draft locally $draftMessageId")
    }
}
