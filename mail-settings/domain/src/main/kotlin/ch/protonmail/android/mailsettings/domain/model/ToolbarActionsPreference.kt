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

import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ActionsToolbarSetting
import me.proton.core.mailsettings.domain.entity.MobileSettings
import me.proton.core.mailsettings.domain.entity.ToolbarAction

data class ToolbarActionsPreference(
    val messageToolbar: ToolbarActions,
    val conversationToolbar: ToolbarActions,
    val listToolbar: ToolbarActions,
    val isConversationMode: Boolean
) {

    val messageOrConvToolbar get() = if (isConversationMode) conversationToolbar else messageToolbar

    data class ActionSelection(
        val selected: List<StringEnum<ToolbarAction>>,
        val all: List<ToolbarAction>
    ) {

        fun canAddMore(): Boolean = recognizedSelectedSize() < Defaults.MAX_ACTIONS
        fun canRemove(): Boolean = recognizedSelectedSize() > Defaults.MIN_ACTIONS

        private fun recognizedSelectedSize() = selected.mapNotNull { it.enum }.size

        fun toggleSelection(actionId: String, toggled: Boolean): ActionSelection {
            val action = Defaults.AllActions.firstOrNull { it.identifier == actionId } ?: return this
            return if (toggled) {
                copy(
                    selected = selected + ToolbarAction.enumOf(action.value)
                )
            } else {
                copy(selected = selected.filterNot { it.value == action.value })
            }
        }

        fun reorder(fromIndex: Int, toIndex: Int): ActionSelection {
            val reorderedRecognized = selected.mapNotNull { it.enum }
                .toMutableList()
                .apply { add(toIndex, removeAt(fromIndex)) }

            var recognizedActionsIdx = 0
            val reordered = selected.map { stringEnum ->
                if (stringEnum.enum == null) {
                    stringEnum
                } else {
                    ToolbarAction.enumOf(reorderedRecognized[recognizedActionsIdx].value).also {
                        recognizedActionsIdx++
                    }
                }
            }
            return copy(
                selected = reordered
            )
        }
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

        fun resetToDefault() = copy(current = current.copy(selected = default.map { ToolbarAction.enumOf(it.value) }))

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
                    .createActions(Defaults.MessageConversationActions, Defaults.AllMessageActions),
                conversationToolbar = from?.conversationToolbar
                    .createActions(
                        Defaults.MessageConversationActions, Defaults.AllConversationActions,
                        Defaults.IgnoredConversationActions
                    ),
                listToolbar = from?.listToolbar
                    .createActions(Defaults.MailboxActions, Defaults.AllMailboxActions),
                isConversationMode = isConversationMode
            )
        }

        private fun ActionsToolbarSetting?.createActions(
            default: List<ToolbarAction>,
            all: List<ToolbarAction>,
            ignored: Set<ToolbarAction> = emptySet()
        ) = ToolbarActions(
            current = ActionSelection(
                selected = this?.actions
                    ?.filterNot { it.enum in ignored }
                    ?.takeIf { action -> action.any { it.enum != null } }
                    ?: default.map { ToolbarAction.enumOf(it.value) },
                all = all
            ),
            default = default
        )
    }

    object Defaults {

        const val MIN_ACTIONS = 1
        const val MAX_ACTIONS = 5

        val IgnoredConversationActions = setOf(
            ToolbarAction.ReplyOrReplyAll,
            ToolbarAction.Forward
        )

        val MessageConversationActions = listOf(
            ToolbarAction.MarkAsReadOrUnread,
            ToolbarAction.MoveToTrash,
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs
        )

        val MailboxActions = listOf(
            ToolbarAction.MarkAsReadOrUnread,
            ToolbarAction.MoveToTrash,
            ToolbarAction.MoveTo,
            ToolbarAction.LabelAs
        )

        val AllConversationActions: List<ToolbarAction> = MessageConversationActions + listOf(
            ToolbarAction.StarOrUnstar,
            ToolbarAction.MoveToArchive,
            ToolbarAction.MoveToSpam,
            ToolbarAction.Print,
            ToolbarAction.ReportPhishing
        )

        val AllMessageActions: List<ToolbarAction> = MessageConversationActions + listOf(
            ToolbarAction.ReplyOrReplyAll,
            ToolbarAction.Forward,
            ToolbarAction.StarOrUnstar,
            ToolbarAction.MoveToArchive,
            ToolbarAction.MoveToSpam,
            ToolbarAction.Print,
            ToolbarAction.ReportPhishing
        )

        val AllMailboxActions: List<ToolbarAction> = MailboxActions + listOf(
            ToolbarAction.StarOrUnstar,
            ToolbarAction.MoveToArchive,
            ToolbarAction.MoveToSpam
        )

        val AllActions = ToolbarAction.entries
    }
}

enum class SettingsToolbarType {
    Message, Inbox
}

val ToolbarAction.identifier: String get() = value
