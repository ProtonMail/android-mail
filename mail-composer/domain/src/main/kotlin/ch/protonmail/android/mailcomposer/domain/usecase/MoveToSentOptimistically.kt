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

import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class MoveToSentOptimistically @Inject constructor(
    private val messageRepository: MessageRepository,
    private val findLocalDraft: FindLocalDraft
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId) {
        val localDraft = findLocalDraft(userId, messageId)
        val localDraftMessageId = if (localDraft != null) {
            localDraft.message.messageId
        } else {
            Timber.e("Local draft not found while trying to move sending message to sent $messageId")
            messageId
        }

        messageRepository.moveMessageFromDraftsToSent(userId, localDraftMessageId).onLeft {
            Timber.e("Failed moving sending message to sent folder optimistically: $it")
        }
    }
}
