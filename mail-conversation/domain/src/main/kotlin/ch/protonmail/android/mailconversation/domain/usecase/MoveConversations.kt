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

package ch.protonmail.android.mailconversation.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.usecase.RegisterUndoableOperation
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MoveConversations @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val observeExclusiveMailLabels: ObserveExclusiveMailLabels,
    private val observeMailLabels: ObserveMailLabels,
    private val incrementUnreadCount: IncrementUnreadCount,
    private val decrementUnreadCount: DecrementUnreadCount,
    private val registerUndoableOperation: RegisterUndoableOperation
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError, Unit> = either {
        val allLabelIds = observeMailLabels(userId).first().allById.mapNotNull { it.key.labelId }
        val exclusiveMailLabels = observeExclusiveMailLabels(userId).first().allById.mapNotNull { it.key.labelId }
        val undoableOperation = defineUndoableOperation(userId, conversationIds, exclusiveMailLabels)
        decrementUnreadConversationsCount(userId, conversationIds, exclusiveMailLabels)
        conversationRepository
            .move(userId, conversationIds, allLabelIds, exclusiveMailLabels, toLabelId = labelId)
            .onRight {
                incrementUnreadConversationsCount(userId, conversationIds, labelId)
                registerUndoableOperation(undoableOperation)
            }
            .bind()
    }

    private suspend fun defineUndoableOperation(
        userId: UserId,
        conversationIds: List<ConversationId>,
        exclusiveLabelIds: List<LabelId>
    ): UndoableOperation {
        val conversationToOriginLabelIdMap = conversationRepository
            .observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.associate { conversation ->
                Pair(
                    conversation.conversationId,
                    conversation.labels.firstOrNull { it.labelId in exclusiveLabelIds }?.labelId
                )
            }

        val labelIdToConversationIds = conversationToOriginLabelIdMap?.keys?.groupBy {
            conversationToOriginLabelIdMap[it]
        }

        return UndoableOperation.UndoMoveConversations {
            labelIdToConversationIds?.forEach { entry ->
                entry.key?.let { labelId ->
                    this@MoveConversations(userId, entry.value, labelId)
                        .onLeft { return@UndoMoveConversations it.left() }
                }
            }
            return@UndoMoveConversations Unit.right()
        }
    }

    private suspend fun decrementUnreadConversationsCount(
        userId: UserId,
        conversationIds: List<ConversationId>,
        fromLabelIds: List<LabelId>
    ) {
        conversationRepository.observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.onEach { conversation ->
                fromLabelIds.forEach { fromLabelId ->
                    if (conversation.hasUnreadMessagesInlabel(fromLabelId)) {
                        decrementUnreadCount(userId, listOf(fromLabelId))
                    }
                }
            }
    }

    private suspend fun incrementUnreadConversationsCount(
        userId: UserId,
        conversationIds: List<ConversationId>,
        toLabelId: LabelId
    ) {
        conversationRepository.observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.onEach { conversation ->
                if (conversation.hasUnreadMessagesInlabel(toLabelId)) {
                    incrementUnreadCount(userId, listOf(toLabelId))
                }
            }
    }

    private fun Conversation.hasUnreadMessagesInlabel(contextLabelId: LabelId) =
        this.labels.find { it.labelId == contextLabelId }.hasUnreadMessages()

    private fun ConversationLabel?.hasUnreadMessages() = this?.let { it.contextNumUnread > 0 } ?: false
}
