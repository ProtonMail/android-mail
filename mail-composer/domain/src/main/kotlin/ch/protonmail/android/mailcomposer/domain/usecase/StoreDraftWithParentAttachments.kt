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

import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.mailcommon.domain.util.mapFalse
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithParentAttachments @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val saveDraft: SaveDraft,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        parentMessage: MessageWithBody,
        senderEmail: SenderEmail,
        draftAction: DraftAction
    ): Either<Error, Unit> = either {
        transactor.performTransaction {

            val parentAttachments = when (draftAction) {
                is DraftAction.Forward -> parentMessage.messageBody.attachments
                is DraftAction.Reply,
                is DraftAction.ReplyAll -> parentMessage.messageBody.attachments.filter { it.disposition == "inline" }
                is DraftAction.Compose -> {
                    Timber.w("Store Draft with parent attachments for a Compose action. This shouldn't happen.")
                    shift(Error.ActionWithNoParent)
                }
            }
            if (parentAttachments.isEmpty()) {
                Timber.d("No attachments to be stored from parent message")
                shift<Error>(Error.NoAttachmentsToBeStored)
            }

            Timber.d("Storing draft for action: $draftAction with parent attachments: $parentAttachments")

            val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
                .mapLeft { Error.DraftDataError }
                .bind()

            val parentAttachmentsWithoutSignature = parentAttachments.map {
                it.copy(signature = null, encSignature = null)
            }
            val updatedDraft = draftWithBody.copy(
                messageBody = draftWithBody.messageBody.copy(
                    attachments = draftWithBody.messageBody.attachments + parentAttachmentsWithoutSignature
                )
            )
            saveDraft(updatedDraft, userId)
                .mapFalse { Error.DraftDataError }
                .bind()
        }

    }

    sealed interface Error {
        object DraftDataError : Error
        object ActionWithNoParent : Error
        object NoAttachmentsToBeStored : Error
    }
}
