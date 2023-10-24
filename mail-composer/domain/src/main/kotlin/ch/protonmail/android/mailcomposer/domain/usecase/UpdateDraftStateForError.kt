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

import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Updates the [DraftState] for the given messageId.
 * When the any error happens while a message is being sent, state is updated to "error sending"
 * and the message is moved back to drafts folder
 * In all other cases, the given newState is applied.
 */
class UpdateDraftStateForError @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        newState: DraftSyncState
    ) {
        val draftState = draftStateRepository.observe(userId, messageId).firstOrNull()?.getOrNull()

        if (draftState?.state == DraftSyncState.Sending) {
            draftStateRepository.updateDraftSyncState(userId, messageId, DraftSyncState.ErrorSending)
            messageRepository.moveMessageBackFromSentToDrafts(userId, messageId)
        } else {
            draftStateRepository.updateDraftSyncState(userId, messageId, newState)
        }
    }
}
