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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.ConversationLabelPropagationOptions
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maildetail.domain.usecase.MoveRemoteMessageAndLocalConversation.PartialMoveError.ConversationLabelingError
import ch.protonmail.android.maildetail.domain.usecase.MoveRemoteMessageAndLocalConversation.PartialMoveError.MessageMoveError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MoveRemoteMessageAndLocalConversation @Inject constructor(
    private val moveMessage: MoveMessage,
    private val conversationRepository: ConversationRepository
) {

    /**
     * Moves the given message (locally + remotely) and the parent conversation (only locally).
     *
     * This UC suits the case where a message in a conversation needs to be moved as such, and the conversation
     * itself needs to reflect the label change state. The relabeling on the conversation level is performed only
     * locally and optimistically as otherwise it would trigger a remote move on a stale conversation state
     * if the user goes back online after a long period of time, which could cause unwanted labeling at the
     * conversation level.
     *
     * The unread counters are not handled in this case since this is expected to be called within the details screen,
     * which expects the user to have the message expanded and marked as read.
     *
     * @param userId the user ID
     * @param messageId the message ID
     * @param conversationId the conversation ID
     * @param conversationLabelingOptions the conversation labeling options
     *
     */
    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        conversationId: ConversationId,
        conversationLabelingOptions: ConversationLabelingOptions
    ): Either<PartialMoveError, Unit> = either {
        moveMessage(userId, messageId, conversationLabelingOptions.toLabel).getOrElse {
            raise(MessageMoveError)
        }

        val conversationLabelPropagationOptions = ConversationLabelPropagationOptions(
            propagateToMessages = false,
            propagateRemotely = false
        )

        if (conversationLabelingOptions.removeCurrentLabel && conversationLabelingOptions.fromLabel != null) {
            conversationRepository.removeLabels(
                userId,
                listOf(conversationId),
                listOf(conversationLabelingOptions.fromLabel),
                conversationLabelPropagationOptions
            ).onLeft { raise(ConversationLabelingError) }
        }

        conversationRepository.addLabels(
            userId,
            listOf(conversationId),
            listOf(conversationLabelingOptions.toLabel),
            conversationLabelPropagationOptions
        ).onLeft {
            raise(ConversationLabelingError)
        }
    }

    sealed interface PartialMoveError {
        data object MessageMoveError : PartialMoveError
        data object ConversationLabelingError : PartialMoveError
    }

    /**
     * Wraps the labeling parameters when performing a "Label As"/"Move To" operation.
     *
     * @param removeCurrentLabel whether the current label should be removed.
     * @param fromLabel the label currently applied before the labeling action.
     * @param toLabel the destination label.
     */
    data class ConversationLabelingOptions(
        val removeCurrentLabel: Boolean,
        val fromLabel: LabelId?,
        val toLabel: LabelId
    )
}
