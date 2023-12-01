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

import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveMessageAttachments @Inject constructor(
    private val draftStateRepository: DraftStateRepository,
    private val messageRepository: MessageRepository
) {

    operator fun invoke(userId: UserId, messageId: MessageId): Flow<List<MessageAttachment>> {
        return draftStateRepository.observe(userId, messageId)
            .distinctUntilChanged()
            .flatMapLatest {
                val draftState = it.getOrNull()
                val currentId = draftState?.apiMessageId ?: messageId
                messageRepository.observeMessageAttachments(userId, currentId)
                    .verifyAssociatedMessageIdForAttachments(userId, currentId)
            }
            .distinctUntilChanged()
    }

    /**
     * This method got introduced since we failed to come up with a better solution
     * The problem is that when the messageId gets updated, room decides which observer is triggered first
     * This is causing that observing the attachments is triggered before the draft state update is emitted
     * Since the messageId changed, the attachments are not associated with the used messageId anymore
     * To avoid flickering in the UI, this method loads the latest version of the draft state
     * and uses the updated apiMessageId, if it exists, to load the attachments
     */
    private fun Flow<List<MessageAttachment>>.verifyAssociatedMessageIdForAttachments(
        userId: UserId,
        messageId: MessageId
    ): Flow<List<MessageAttachment>> = this.map {
        it.ifEmpty {
            val currentDraftState = draftStateRepository.observe(userId, messageId).first().getOrNull()
                ?: return@map emptyList()
            val latestMessageID = currentDraftState.apiMessageId ?: messageId
            messageRepository.observeMessageAttachments(userId, latestMessageID).first()
        }
    }
}
