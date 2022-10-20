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
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveMessageDetailActions @Inject constructor(
    private val observeMessage: ObserveMessage
) {

    operator fun invoke(userId: UserId, messageId: MessageId): Flow<Either<DataError, List<Action>>> =
        observeMessage(userId, messageId).mapLatest { either ->
            either.map { message ->
                val actions = BottomBarDefaults.Message.actions.toMutableList()

                if (message.messageIsSpamOrTrash()) {
                    actions[actions.indexOf(Action.Trash)] = Action.Delete
                }
                if (message.hasMultipleRecipients()) {
                    actions[actions.indexOf(Action.Reply)] = Action.ReplyAll
                }
                actions
            }
        }

    private fun Message.hasMultipleRecipients() = (toList + ccList).size > 1

    private fun Message.messageIsSpamOrTrash() = labelIds.any {
        it == SystemLabelId.Spam.labelId || it == SystemLabelId.Trash.labelId
    }
}
