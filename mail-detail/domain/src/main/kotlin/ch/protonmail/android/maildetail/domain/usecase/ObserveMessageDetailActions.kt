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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.model.BottomBarDefaults
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailMessageToolbarSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveMessageDetailActions @Inject constructor(
    private val observeMessage: ObserveMessage,
    private val observeToolbarActions: ObserveMailMessageToolbarSettings
) {

    operator fun invoke(userId: UserId, messageId: MessageId): Flow<Either<DataError, List<Action>>> = combine(
        observeMessage(userId, messageId),
        observeToolbarActions(userId, isMailBox = false)
    ) { either, toolbarActions ->
        either.map { message ->
            val actions = (toolbarActions ?: BottomBarDefaults.Message.actions).toMutableList()

            val hasMultipleRecipients = message.allRecipientsDeduplicated.size > 1
            if (hasMultipleRecipients) {
                actions.replace(Action.Reply, with = Action.ReplyAll)
            }

            if (message.messageIsSpamOrTrash()) {
                actions.replace(Action.Trash, with = Action.Delete) // permanently delete for spam/trash
                actions.replace(Action.Spam, with = Action.Move) // move to inbox (not spam) for spam/trash
            } else {
                actions.replace(Action.Delete, with = Action.Trash) // delete (not permanent) for non-spam/non-trash
            }
            if (message.isStarred()) {
                actions.replace(Action.Star, with = Action.Unstar)
            } else {
                actions.replace(Action.Unstar, with = Action.Star)
            }
            actions.add(Action.More)
            actions.distinct()
        }
    }

    private fun MutableList<Action>.replace(action: Action, with: Action) {
        val index = indexOf(action).takeIf { it >= 0 } ?: return
        set(index, with)
    }

    private fun Message.messageIsSpamOrTrash() = labelIds.any {
        it == SystemLabelId.Spam.labelId || it == SystemLabelId.Trash.labelId
    }

    private fun Message.isStarred() = labelIds.any {
        it == SystemLabelId.Starred.labelId
    }
}
