/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.repository.EphemeralMailboxCursorRepository
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The owner of a scroller invokes this command before it's lifecycle is completed so that other screens may
 * access a valid cursor for the scroller. The Cursor is managed and will self terminate without any subscribers
 * within a certain timeframe meaning that the caller does not need to bother with cleanup
 */
class SetEphemeralMailboxCursor @Inject constructor(
    val conversationRepository: ConversationRepository,
    val messageRepository: MessageRepository,
    val ephemeralMailboxCursorRepository: EphemeralMailboxCursorRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        viewModeIsConversation: Boolean,
        cursorId: CursorId,
        labelId: LabelId
    ) {
        when (viewModeIsConversation) {
            true -> {
                conversationRepository.getConversationCursor(
                    userId = userId,
                    firstPage = cursorId,
                    labelId = labelId
                )
            }

            false -> {
                messageRepository.getConversationCursor(userId = userId, firstPage = cursorId, labelId = labelId)
            }
        }.onLeft {
            Timber.d("conversation-cursor unable to get cursor error $it")
        }.onRight {
            Timber.d("conversation-cursor got EphemeralCursor $it")
            ephemeralMailboxCursorRepository.setEphemeralCursor(it)
        }
    }
}
