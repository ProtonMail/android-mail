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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import javax.inject.Inject
import ch.protonmail.android.maillabel.presentation.R as LabelR

class ActionResultMapper @Inject constructor(
    private val mailLabelTextMapper: MailLabelTextMapper
) {

    fun toActionResult(operation: ConversationDetailOperation): ActionResult? = when (operation) {
        is ConversationDetailViewAction.MoveToArchive ->
            UndoableActionResult(TextUiModel(R.string.conversation_moved_to_archive))

        is ConversationDetailViewAction.MoveToSpam ->
            UndoableActionResult(TextUiModel(R.string.conversation_moved_to_spam))

        is ConversationDetailViewAction.MoveToTrash ->
            UndoableActionResult(TextUiModel(R.string.conversation_moved_to_trash))

        is ConversationDetailViewAction.MoveToInbox ->
            UndoableActionResult(TextUiModel(R.string.conversation_moved_to_inbox))

        is ConversationDetailViewAction.MoveToCompleted -> toMoveToActionResult(operation)

        is ConversationDetailViewAction.LabelAsCompleted -> if (operation.wasArchived) {
            val textRes = when (operation.entryPoint) {
                is LabelAsBottomSheetEntryPoint.Conversation -> R.string.conversation_moved_to_archive
                is LabelAsBottomSheetEntryPoint.Message -> R.string.message_moved_to_archive
                else -> null
            }

            textRes?.let { UndoableActionResult(TextUiModel(it)) }
        } else null

        is ConversationDetailViewAction.DeleteConfirmed ->
            DefinitiveActionResult(TextUiModel(R.string.conversation_deleted))

        is ConversationDetailEvent.MessageMoved -> {
            val textUiModel = TextUiModel.TextResWithArgs(
                value = R.string.message_moved_to,
                formatArgs = listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
            )

            UndoableActionResult(textUiModel)
        }

        is ConversationDetailEvent.LastMessageMoved -> {
            val textUiModel = TextUiModel.TextResWithArgs(
                value = R.string.message_moved_to,
                formatArgs = listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
            )

            UndoableActionResult(textUiModel)
        }

        is ConversationDetailEvent.LastMessageDeleted -> {
            DefinitiveActionResult(TextUiModel(R.string.message_deleted))
        }

        is ConversationDetailViewAction.SnoozeCompleted ->
            DefinitiveActionResult(TextUiModel(operation.completeMessage))

        is ConversationDetailEvent.UnsnoozeCompleted ->
            DefinitiveActionResult(TextUiModel(value = R.string.unsnooze_sheet_success))

        else -> null // No specific ActionResult for other operations
    }

    private fun toMoveToActionResult(operation: ConversationDetailViewAction.MoveToCompleted): ActionResult? {
        val textRes = when {
            operation.isCategory -> LabelR.string.move_to_category_action_message
            operation.entryPoint is MoveToBottomSheetEntryPoint.Conversation ->
                R.string.conversation_moved_to_selected_destination

            operation.entryPoint is MoveToBottomSheetEntryPoint.Message ->
                R.string.message_moved_to_selected_destination

            else -> null
        }

        return textRes?.let {
            UndoableActionResult(
                message = TextUiModel.TextResWithArgs(
                    value = it,
                    formatArgs = listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
                )
            )
        }
    }
}
