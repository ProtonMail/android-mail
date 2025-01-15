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

package ch.protonmail.android.mailsettings.domain.model

import me.proton.core.mailsettings.domain.entity.ActionsToolbarSetting
import me.proton.core.mailsettings.domain.entity.MobileSettings
import me.proton.core.mailsettings.domain.entity.ToolbarAction

data class ToolbarActionsPreference(
    val messageToolbar: ToolbarActions,
    val conversationToolbar: ToolbarActions,
    val listToolbar: ToolbarActions,
    val isConversationMode: Boolean
) {

    data class ActionSelection(
        val selected: List<ToolbarAction>,
        val all: List<ToolbarAction>
    ) {

        fun canAddMore(): Boolean = selected.size < Defaults.MAX_ACTIONS
        fun canRemove(): Boolean = selected.size > Defaults.MIN_ACTIONS

        fun toggleSelection(actionId: String, toggled: Boolean): ActionSelection {
            val action = all.firstOrNull { it.identifier == actionId } ?: return this
            return if (toggled) {
                copy(
                    selected = selected + action
                )
            } else {
                copy(selected = selected.filterNot { it == action })
            }
        }

        fun reorder(fromIndex: Int, toIndex: Int): ActionSelection = copy(
            selected = selected.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }.toList()
        )
    }

    fun update(toolbarType: SettingsToolbarType, block: (ToolbarActions) -> ToolbarActions) = when (toolbarType) {
        SettingsToolbarType.Inbox -> copy(listToolbar = block(listToolbar))
        SettingsToolbarType.Message -> if (isConversationMode) {
            copy(conversationToolbar = block(conversationToolbar))
        } else {
            copy(messageToolbar = block(messageToolbar))
        }
    }

    data class ToolbarActions(
        val current: ActionSelection,
        val default: List<ToolbarAction>
    ) {

        fun resetToDefault() = copy(current = current.copy(selected = default))

        fun reorder(fromIndex: Int, toIndex: Int) = copy(
            current = current.reorder(fromIndex = fromIndex, toIndex = toIndex)
        )

        fun toggleSelection(actionId: String, toggled: Boolean) =
            copy(current = current.toggleSelection(actionId, toggled))
    }

    companion object {

        fun create(from: MobileSettings?, isConversationMode: Boolean): ToolbarActionsPreference {
            return ToolbarActionsPreference(
                messageToolbar = from?.messageToolbar
                    .createActions(Defaults.MessageActions, Defaults.AllMessageActions),
                conversationToolbar = from?.conversationToolbar
                    .createActions(Defaults.MessageActions, Defaults.AllMessageActions),
                listToolbar = from?.listToolbar
                    .createActions(Defaults.InboxActions, Defaults.AllInboxActions),
                isConversationMode = isConversationMode
            )
        }

        private fun ActionsToolbarSetting?.createActions(default: List<ToolbarAction>, all: List<ToolbarAction>) =
            ToolbarActions(
                current = ActionSelection(
                    selected = this?.actions?.mapNotNull { it.enum } ?: default,
                    all = all
                ),
                default = default
            )
    }

    object Defaults {

        const val MIN_ACTIONS = 1
        const val MAX_ACTIONS = 5

        val MessageActions = listOf(
            ToolbarAction.MarkAsReadOrUnread,
            ToolbarAction.MoveToTrash,
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs
        )

        val InboxActions = listOf(
            ToolbarAction.MarkAsReadOrUnread,
            ToolbarAction.MoveToTrash,
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs,
            ToolbarAction.MoveToSpam
        )

        val AllMessageActions: List<ToolbarAction> = listOf(
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs,
            ToolbarAction.ReplyOrReplyAll,
            ToolbarAction.Forward,
            ToolbarAction.StarOrUnstar,
            ToolbarAction.MoveToArchive,
            ToolbarAction.Print,
            ToolbarAction.ReportPhishing
        )

        val AllInboxActions: List<ToolbarAction> = listOf(
            ToolbarAction.MarkAsReadOrUnread,
            ToolbarAction.MoveToTrash,
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs,
            ToolbarAction.MoveToSpam,
            ToolbarAction.StarOrUnstar,
            ToolbarAction.MoveToArchive
        )
    }
}

enum class SettingsToolbarType {
    Message, Inbox
}

val ToolbarAction.identifier: String get() = value
