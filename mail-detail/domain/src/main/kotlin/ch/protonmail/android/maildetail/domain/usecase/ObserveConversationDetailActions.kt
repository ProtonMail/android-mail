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
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.model.BottomBarDefaults
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailMessageToolbarSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveConversationDetailActions @Inject constructor(
    private val observeConversation: ObserveConversation,
    private val observeToolbarActions: ObserveMailMessageToolbarSettings,
    private val observeMessages: ObserveConversationMessagesWithLabels
) {

    operator fun invoke(
        userId: UserId,
        conversationId: ConversationId,
        refreshConversations: Boolean
    ): Flow<Either<DataError, List<Action>>> = combine(
        observeConversation(userId, conversationId, refreshConversations),
        observeToolbarActions(userId, isMailBox = false),
        observeMessages(userId, conversationId)
    ) { either, toolbarActions, messagesEither ->
        either.map { conversation ->
            val actions = (toolbarActions ?: BottomBarDefaults.Conversation.actions).toMutableList()

            if (conversation.allMessagesAreSpamOrTrash()) {
                actions.replace(Action.Trash, with = Action.Delete) // permanently delete for spam/trash
                actions.replace(Action.Spam, with = Action.Move) // move to inbox (not spam) for spam/trash
            } else {
                actions.replace(Action.Delete, with = Action.Trash) // delete (not permanent) for non-spam/non-trash
            }
            if (conversation.anyMessageStarred()) {
                actions.replace(Action.Star, with = Action.Unstar)
            }
            if (conversation.areAllMessagesArchived()) {
                actions.replace(Action.Archive, with = Action.Move)
            }
            if (actions.contains(Action.Reply)) {
                val hasMultipleRecipients = messagesEither.getOrNull()?.lastOrNull {
                    it.message.isDraft().not()
                }?.message?.allRecipientsDeduplicated?.size?.let { it > 1 } ?: false
                if (hasMultipleRecipients) {
                    actions.replace(Action.Reply, with = Action.ReplyAll)
                }
            }
            actions.add(Action.More)
            actions.distinct()
        }
    }

    private fun MutableList<Action>.replace(action: Action, with: Action) {
        val index = indexOf(action).takeIf { it >= 0 } ?: return
        set(index, with)
    }

    private fun Conversation.allMessagesAreSpamOrTrash() = labels
        .ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .areAllTrashOrSpam()

    private fun Conversation.areAllMessagesArchived() = labels
        .ignoringAllMail()
        .ignoringAlmostAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .areAllArchive()

    private fun Conversation.anyMessageStarred() = labels
        .ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .isAnyStarred()

    private fun List<ConversationLabel>.areAllTrashOrSpam() = this.all {
        it.labelId == SystemLabelId.Spam.labelId || it.labelId == SystemLabelId.Trash.labelId
    }

    private fun List<ConversationLabel>.areAllArchive() = this.all {
        it.labelId == SystemLabelId.Archive.labelId
    }

    private fun List<ConversationLabel>.isAnyStarred() = this.any {
        it.labelId == SystemLabelId.Starred.labelId
    }

    private fun List<ConversationLabel>.ignoringAllMail() = this.filterNot {
        it.labelId == SystemLabelId.AllMail.labelId
    }

    private fun List<ConversationLabel>.ignoringAlmostAllMail() = this.filterNot {
        it.labelId == SystemLabelId.AlmostAllMail.labelId
    }

    private fun List<ConversationLabel>.ignoringAllSent() = this.filterNot {
        it.labelId == SystemLabelId.AllSent.labelId
    }

    private fun List<ConversationLabel>.ignoringAllDrafts() = this.filterNot {
        it.labelId == SystemLabelId.AllDrafts.labelId
    }
}
