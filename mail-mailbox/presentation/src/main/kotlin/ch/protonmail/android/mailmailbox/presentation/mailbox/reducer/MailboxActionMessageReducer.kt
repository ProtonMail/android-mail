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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import javax.inject.Inject

class MailboxActionMessageReducer @Inject constructor(
    private val mailLabelTextMapper: MailLabelTextMapper
) {

    internal fun newStateFrom(operation: MailboxOperation.AffectingActionMessage): Effect<ActionResult> {
        val actionResult = when (operation) {
            is MailboxEvent.MoveToConfirmed.Category -> moveResult(
                operation = operation,
                conversationRes = R.plurals.mailbox_action_move_conversation_to_category,
                messageRes = R.plurals.mailbox_action_move_message_to_category
            )

            is MailboxEvent.MoveToConfirmed -> moveResult(
                operation = operation,
                conversationRes = R.plurals.mailbox_action_move_conversation,
                messageRes = R.plurals.mailbox_action_move_message
            )

            is MailboxEvent.LabelAsConfirmed -> {
                // Do not show a snackbar on labeling with no archive
                if (!operation.alsoArchived) return Effect.empty()

                val pluralsRes = when (operation.viewMode) {
                    ViewMode.ConversationGrouping -> R.plurals.mailbox_action_move_conversation
                    ViewMode.NoConversationGrouping -> R.plurals.mailbox_action_move_message
                }

                val labelText = MailLabelText(R.string.label_title_archive)
                val destinationFolder = mailLabelTextMapper.mapToString(labelText)

                UndoableActionResult(
                    TextUiModel.PluralisedText(
                        pluralsRes,
                        operation.itemCount,
                        listOf(destinationFolder)
                    )
                )
            }

            is MailboxEvent.DeleteConfirmed -> {
                val resource = when (operation.viewMode) {
                    ViewMode.ConversationGrouping -> R.plurals.mailbox_action_delete_conversation
                    ViewMode.NoConversationGrouping -> R.plurals.mailbox_action_delete_message
                }
                DefinitiveActionResult(TextUiModel(resource, operation.numAffectedMessages))
            }

            is MailboxViewAction.SwipeArchiveAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_archive_message)
            )

            is MailboxViewAction.SwipeSpamAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_spam_message)
            )

            is MailboxViewAction.SwipeTrashAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_trash_message)
            )

            is MailboxEvent.MaxSelectionLimitReached ->
                DefinitiveActionResult(TextUiModel(R.string.mailbox_action_maximum_selection_reached))
        }
        return Effect.of(actionResult)
    }

    private fun moveResult(
        operation: MailboxEvent.MoveToConfirmed,
        conversationRes: Int,
        messageRes: Int
    ): UndoableActionResult {
        val pluralsRes = when (operation.viewMode) {
            ViewMode.ConversationGrouping -> conversationRes
            ViewMode.NoConversationGrouping -> messageRes
        }

        val destination = mailLabelTextMapper.mapToString(operation.label)

        return UndoableActionResult(
            TextUiModel.PluralisedText(pluralsRes, operation.itemCount, listOf(destination))
        )
    }
}
