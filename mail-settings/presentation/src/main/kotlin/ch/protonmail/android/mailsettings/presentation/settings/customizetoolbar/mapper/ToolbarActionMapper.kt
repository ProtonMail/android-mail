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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.mapper

import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.CustomizeToolbarState
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ToolbarActionUiModel
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import javax.inject.Inject
import ch.protonmail.android.mailsettings.presentation.R as presentationR

class ToolbarActionMapper @Inject constructor() {

    fun mapToUI(
        isMailbox: Boolean,
        isInConversationMode: Boolean,
        selection: ToolbarActionsPreference.ActionSelection
    ): CustomizeToolbarState.Data.Page = with(selection) {
        val selectedEnabled = canRemove()
        val remainingEnabled = canAddMore()
        val recognizedSelected = selected.mapNotNull { it.enum }
        val remaining = all.filterNot { recognizedSelected.contains(it) }
        CustomizeToolbarState.Data.Page(
            disclaimer = TextUiModel.TextRes(
                when (isMailbox) {
                    false -> if (isInConversationMode) {
                        presentationR.string.customize_toolbar_disclaimer_conversation
                    } else {
                        presentationR.string.customize_toolbar_disclaimer_message
                    }
                    true -> if (isInConversationMode) {
                        presentationR.string.customize_toolbar_disclaimer_mailbox_conversations
                    } else {
                        presentationR.string.customize_toolbar_disclaimer_mailbox
                    }
                }
            ),
            selectedActions = recognizedSelected.map { toUiModel(it, enabled = selectedEnabled) },
            remainingActions = remaining.map { toUiModel(it, enabled = remainingEnabled) }
        )
    }

    private fun toUiModel(toolbarAction: ToolbarAction, enabled: Boolean): ToolbarActionUiModel = with(toolbarAction) {
        val (desc, icon) = when (this) {
            ToolbarAction.ReplyOrReplyAll -> R.string.action_reply_description to R.drawable.ic_proton_reply
            ToolbarAction.Forward -> R.string.action_forward_description to R.drawable.ic_proton_forward
            ToolbarAction.MarkAsReadOrUnread ->
                R.string.action_mark_unread_description to R.drawable.ic_proton_envelope_dot

            ToolbarAction.StarOrUnstar -> R.string.action_star_description to R.drawable.ic_proton_star
            ToolbarAction.LabelAs -> R.string.action_label_description to R.drawable.ic_proton_tag
            ToolbarAction.MoveTo -> R.string.action_move_description to R.drawable.ic_proton_folder_arrow_in
            ToolbarAction.MoveToTrash -> R.string.action_trash_description to R.drawable.ic_proton_trash
            ToolbarAction.MoveToArchive -> R.string.action_archive_description to R.drawable.ic_proton_archive_box
            ToolbarAction.MoveToSpam -> R.string.action_spam_description to R.drawable.ic_proton_fire
            ToolbarAction.ViewMessageInLightMode ->
                R.string.action_view_in_light_mode_description to R.drawable.ic_proton_sun

            ToolbarAction.Print -> R.string.action_print_description to R.drawable.ic_proton_printer
            ToolbarAction.ReportPhishing -> R.string.action_report_phishing_description to R.drawable.ic_proton_hook
        }
        return ToolbarActionUiModel(
            id = value,
            icon = icon,
            description = TextUiModel.TextRes(desc),
            enabled = enabled
        )
    }
}
