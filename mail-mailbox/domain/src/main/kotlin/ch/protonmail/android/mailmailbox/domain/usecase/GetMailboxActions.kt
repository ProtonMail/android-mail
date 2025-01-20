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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxBottomBarDefaults
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMailMessageToolbarSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetMailboxActions @Inject constructor(
    private val observeToolbarActions: ObserveMailMessageToolbarSettings
) {

    operator fun invoke(
        currentMailLabel: MailLabel,
        areAllItemsUnread: Boolean,
        userId: UserId
    ): Flow<Either<DataError, List<Action>>> = observeToolbarActions.invoke(userId, isMailBox = true)
        .map { preferences ->
            either {
                val actions = (preferences ?: MailboxBottomBarDefaults.actions).toMutableList()

                if (areAllItemsUnread) {
                    actions[actions.indexOf(Action.MarkUnread)] = Action.MarkRead
                }

                if (currentMailLabel.isTrashOrSpam()) {
                    actions[actions.indexOf(Action.Trash)] = Action.Delete
                }
                actions
            }
        }

    private fun MailLabel.isTrashOrSpam() =
        id.labelId == SystemLabelId.Trash.labelId || id.labelId == SystemLabelId.Spam.labelId
}
