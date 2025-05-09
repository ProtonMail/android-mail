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

import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class SendMessage @Inject constructor(
    private val storeDraftWithAllFields: StoreDraftWithAllFields,
    private val messageRepository: MessageRepository,
    private val draftStateRepository: DraftStateRepository,
    private val moveToSentOptimistically: MoveToSentOptimistically,
    private val injectAddressPublicKeyIntoMessage: InjectAddressPublicKeyIntoMessage,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        fields: DraftFields,
        action: DraftAction = DraftAction.Compose
    ) {
        transactor.performTransaction {
            storeDraftWithAllFields(
                userId,
                messageId,
                fields,
                action
            ).onLeft {
                Timber.e("SendMessage: failed to store draft with all fields: $it")
            }

            draftStateRepository.updateDraftSyncState(userId, messageId, DraftSyncState.Sending, null).onLeft {
                Timber.e("SendMessage: error updating draft sync state: $it")
            }

            moveToSentOptimistically(userId, messageId)
            injectAddressPublicKeyIntoMessage(userId, messageId).onLeft {
                Timber.e("SendMessage: error injecting public key: $it")
            }
        }
        messageRepository.send(userId, messageId)
    }
}
