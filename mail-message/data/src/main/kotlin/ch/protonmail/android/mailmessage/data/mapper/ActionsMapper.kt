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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalLabelAsAction
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.data.mapper.toSystemLabel
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelAsActions
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import timber.log.Timber
import uniffi.mail_uniffi.AllConversationActions
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.AllMessageActions
import uniffi.mail_uniffi.ConversationAction
import uniffi.mail_uniffi.IsSelected
import uniffi.mail_uniffi.ListActions
import uniffi.mail_uniffi.MessageAction
import uniffi.mail_uniffi.MessageActionSheet
import uniffi.mail_uniffi.MoveAction

fun List<LocalLabelAsAction>.toLabelAsActions(): LabelAsActions {
    val labels = this.map { it.toLabel() }
    val selectedLabelIds = this
        .filter { it.isSelected == IsSelected.SELECTED }
        .map { it.labelId.toLabelId() }
    val partiallySelectedLabelIds = this
        .filter { it.isSelected == IsSelected.PARTIAL }
        .map { it.labelId.toLabelId() }

    return LabelAsActions(labels, selectedLabelIds, partiallySelectedLabelIds)
}

private fun LocalLabelAsAction.toLabel() = Label(
    labelId = this.labelId.toLabelId(),
    parentId = null,
    name = this.name,
    type = LabelType.MessageLabel,
    path = "",
    color = this.color.value,
    order = this.order.toInt(),
    isNotified = null,
    isExpanded = null,
    isSticky = null
)

fun List<MoveAction.SystemFolder>.toMailLabels() = this.map { systemAction ->
    MailLabel.System(
        id = MailLabelId.System(systemAction.v1.localId.toLabelId()),
        systemLabelId = systemAction.v1.name.toSystemLabel(),
        order = 0
    )
}

fun MessageActionSheet.toAvailableActions(): AvailableActions {
    return AvailableActions(
        this.replyActions.replyMessageActionsToActions().filterNotNull(),
        this.messageActions.messageActionsToActions().filterNotNull(),
        this.moveActions.systemFolderMessageActionsToActions().filterNotNull(),
        this.generalActions.generalMessageActionsToActions().filterNotNull()
    )
}

fun List<MessageAction>.replyMessageActionsToActions() = this.map { action ->
    when (action) {
        MessageAction.Reply -> Action.Reply
        MessageAction.ReplyAll -> Action.ReplyAll
        MessageAction.Forward -> Action.Forward
        else -> {
            Timber.e("Unexpected action $action passed as ReplyAction")
            null
        }
    }
}

fun List<MessageAction>.systemFolderMessageActionsToActions() = this.map { action ->
    when (action) {
        MessageAction.MoveTo -> Action.Move
        is MessageAction.MoveToSystemFolder -> action.v1.name.toAction()
        is MessageAction.NotSpam -> Action.Inbox
        MessageAction.PermanentDelete -> Action.Delete
        else -> {
            Timber.e("Unexpected action $action passed as SystemFolderAction")
            null
        }
    }
}

fun List<MessageAction>.generalMessageActionsToActions() = this.map { generalAction ->
    when (generalAction) {
        MessageAction.ViewInLightMode -> Action.ViewInLightMode
        MessageAction.ViewInDarkMode -> Action.ViewInDarkMode
        MessageAction.Print -> Action.Print
        MessageAction.ReportPhishing -> Action.ReportPhishing
        MessageAction.ViewHeaders -> Action.ViewHeaders
        MessageAction.ViewHtml -> Action.ViewHtml
        else -> {
            Timber.d("rust-actions-mapper: Skipping unhandled action mapping generalActions: $generalAction")
            null
        }
    }
}

private fun List<MessageAction>.messageActionsToActions() = this.map { messageAction ->
    when (messageAction) {
        MessageAction.Star -> Action.Star
        MessageAction.Unstar -> Action.Unstar
        MessageAction.LabelAs -> Action.Label
        MessageAction.MarkRead -> Action.MarkRead
        MessageAction.MarkUnread -> Action.MarkUnread
        MessageAction.PermanentDelete -> Action.Delete
        else -> {
            Timber.e("Unexpected action $messageAction passed as MessageAction")
            null
        }
    }
}

fun AllListActions.toAllBottomBarActions(): AllBottomBarActions {
    return AllBottomBarActions(
        this.hiddenListActions.bottomBarListActionsToActions(),
        this.visibleListActions.bottomBarListActionsToActions()
    )
}

fun AllConversationActions.toAllBottomBarActions(): AllBottomBarActions {
    return AllBottomBarActions(
        this.hiddenListActions.bottomBarConversationActionsToActions(),
        this.visibleListActions.bottomBarConversationActionsToActions()
    )
}

fun AllMessageActions.toAllBottomBarActions(): AllBottomBarActions {
    return AllBottomBarActions(
        this.hiddenMessageActions.bottomBarActionsToActions(),
        this.visibleMessageActions.bottomBarActionsToActions()
    )
}

private fun List<ListActions>.bottomBarListActionsToActions() = this.map { bottomBarAction ->
    when (bottomBarAction) {
        ListActions.LabelAs -> Action.Label
        ListActions.MarkRead -> Action.MarkRead
        ListActions.MarkUnread -> Action.MarkUnread
        ListActions.More -> Action.More
        ListActions.MoveTo -> Action.Move
        is ListActions.MoveToSystemFolder -> bottomBarAction.v1.name.toAction()
        ListActions.PermanentDelete -> Action.Delete
        ListActions.Star -> Action.Star
        ListActions.Unstar -> Action.Unstar
        is ListActions.NotSpam -> Action.Inbox
        ListActions.Snooze -> Action.Snooze
    }
}


private fun List<ConversationAction>.bottomBarConversationActionsToActions() = this.map { bottomBarAction ->
    when (bottomBarAction) {
        ConversationAction.LabelAs -> Action.Label
        ConversationAction.MarkRead -> Action.MarkRead
        ConversationAction.MarkUnread -> Action.MarkUnread
        ConversationAction.More -> Action.More
        ConversationAction.MoveTo -> Action.Move
        is ConversationAction.MoveToSystemFolder -> bottomBarAction.v1.name.toAction()
        ConversationAction.PermanentDelete -> Action.Delete
        ConversationAction.Star -> Action.Star
        ConversationAction.Unstar -> Action.Unstar
        is ConversationAction.NotSpam -> Action.Inbox
        ConversationAction.Snooze -> Action.Snooze
    }
}

private fun List<MessageAction>.bottomBarActionsToActions() = this.map { bottomBarAction ->
    when (bottomBarAction) {
        MessageAction.Forward -> Action.Forward
        MessageAction.LabelAs -> Action.Label
        MessageAction.MarkRead -> Action.MarkRead
        MessageAction.MarkUnread -> Action.MarkUnread
        MessageAction.More -> Action.More
        is MessageAction.MoveToSystemFolder -> bottomBarAction.v1.name.toAction()
        MessageAction.MoveTo -> Action.Move
        is MessageAction.NotSpam -> Action.Inbox
        MessageAction.PermanentDelete -> Action.Delete
        MessageAction.Print -> Action.Print
        MessageAction.Reply -> Action.Reply
        MessageAction.ReplyAll -> Action.ReplyAll
        MessageAction.ReportPhishing -> Action.ReportPhishing
        MessageAction.Star -> Action.Star
        MessageAction.Unstar -> Action.Unstar
        MessageAction.ViewHeaders -> Action.ViewHeaders
        MessageAction.ViewHtml -> Action.ViewHtml
        MessageAction.ViewInDarkMode -> Action.ViewInDarkMode
        MessageAction.ViewInLightMode -> Action.ViewInLightMode
    }
}

