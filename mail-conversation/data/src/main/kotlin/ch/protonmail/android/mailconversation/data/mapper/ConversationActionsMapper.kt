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

package ch.protonmail.android.mailconversation.data.mapper

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.mailmessage.data.mapper.toAction
import uniffi.mail_uniffi.ConversationAction
import uniffi.mail_uniffi.ConversationActionSheet

fun ConversationActionSheet.toAvailableActions(): AvailableActions {
    return AvailableActions(
        emptyList(),
        this.conversationActions.toActions(),
        this.moveActions.toActions(),
        emptyList()
    )
}

private fun List<ConversationAction>.toActions() = this.map { messageAction ->
    when (messageAction) {
        ConversationAction.LabelAs -> Action.Label
        ConversationAction.MarkRead -> Action.MarkRead
        ConversationAction.MarkUnread -> Action.MarkUnread
        ConversationAction.More -> Action.More
        ConversationAction.MoveTo -> Action.Move
        is ConversationAction.MoveToSystemFolder -> messageAction.v1.name.toAction()
        is ConversationAction.NotSpam -> Action.Inbox
        ConversationAction.PermanentDelete -> Action.Delete
        ConversationAction.Snooze -> Action.Snooze
        ConversationAction.Star -> Action.Star
        ConversationAction.Unstar -> Action.Unstar
    }
}
