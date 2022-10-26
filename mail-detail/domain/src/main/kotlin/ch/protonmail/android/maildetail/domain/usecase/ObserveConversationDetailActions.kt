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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveConversationDetailActions @Inject constructor(
    private val observeConversation: ObserveConversation
) {

    operator fun invoke(userId: UserId, conversationId: ConversationId): Flow<Either<DataError, List<Action>>> =
        observeConversation(userId, conversationId).mapLatest { either ->
            either.map { conversation ->
                val actions = BottomBarDefaults.Conversation.actions.toMutableList()

                if (conversation.allMessagesAreSpamOrTrash()) {
                    actions[actions.indexOf(Action.Trash)] = Action.Delete
                }
                actions
            }
        }

    private fun Conversation.allMessagesAreSpamOrTrash() = labels
        .ignoringAllMail()
        .ignoringAllSent()
        .ignoringAllDrafts()
        .areAllTrashOrSpam()

    private fun List<ConversationLabel>.areAllTrashOrSpam() = this.all {
        it.labelId == SystemLabelId.Spam.labelId || it.labelId == SystemLabelId.Trash.labelId
    }

    private fun List<ConversationLabel>.ignoringAllMail() = this.filterNot {
        it.labelId == SystemLabelId.AllMail.labelId
    }

    private fun List<ConversationLabel>.ignoringAllSent() = this.filterNot {
        it.labelId == SystemLabelId.AllSent.labelId
    }

    private fun List<ConversationLabel>.ignoringAllDrafts() = this.filterNot {
        it.labelId == SystemLabelId.AllDrafts.labelId
    }
}
