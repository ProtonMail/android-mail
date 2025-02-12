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
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.util.mapFalse
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@Deprecated("Part of Composer V1, to be removed")
class StoreDraftWithRecipients @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val saveDraft: SaveDraft,
    private val transactor: Transactor
) {
    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        to: List<Recipient>? = null,
        cc: List<Recipient>? = null,
        bcc: List<Recipient>? = null
    ): Either<Error, Unit> = either {
        transactor.performTransaction {
            val draftWithBody = getLocalDraft(userId, messageId, senderEmail)
                .mapLeft { Error.DraftReadError }
                .bind()

            val updatedDraft = draftWithBody.copy(
                message = draftWithBody.message.copy(
                    toList = to ?: draftWithBody.message.toList,
                    ccList = cc ?: draftWithBody.message.ccList,
                    bccList = bcc ?: draftWithBody.message.bccList
                )
            )
            saveDraft(updatedDraft, userId)
                .mapFalse { Error.DraftSaveError }
                .bind()
        }
    }

    @Deprecated("Part of Composer V1, to be removed")
    sealed interface Error {
        object DraftSaveError : Error
        object DraftReadError : Error
    }
}
