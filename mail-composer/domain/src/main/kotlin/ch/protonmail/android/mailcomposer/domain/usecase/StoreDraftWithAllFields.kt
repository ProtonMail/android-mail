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
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class StoreDraftWithAllFields @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val storeDraftWithSubject: StoreDraftWithSubject,
    private val storeDraftWithBody: StoreDraftWithBody,
    private val storeDraftWithRecipients: StoreDraftWithRecipients,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        draftMessageId: MessageId,
        fields: DraftFields,
        action: DraftAction = DraftAction.Compose
    ) = withContext(NonCancellable) {
        transactor.performTransaction {
            storeDraftWithBody(
                draftMessageId,
                fields.body,
                fields.quotedHtmlBody,
                fields.sender,
                userId
            ).logError(draftMessageId)
            storeDraftWithSubject(userId, draftMessageId, fields.sender, fields.subject).logError(draftMessageId)
            storeDraftWithRecipients(
                userId,
                draftMessageId,
                fields.sender,
                fields.recipientsTo.value,
                fields.recipientsCc.value,
                fields.recipientsBcc.value
            ).logError(draftMessageId)

            draftStateRepository.createOrUpdateLocalState(userId, draftMessageId, action)
            Timber.d("Draft: finished storing draft locally $draftMessageId")
        }
    }

    private fun <T> Either<T, Unit>.logError(draftMessageId: MessageId) = this.onLeft { error ->
        Timber.e(
            "Storing all draft fields failed due to $error. \n Draft MessageId = $draftMessageId"
        )
    }
}
