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
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.maillabel.presentation.R as LabelR
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import javax.inject.Inject

class MailboxActionMessageReducer @Inject constructor(
    private val bulkActionMessageFactory: BulkActionMessageFactory,
    private val mailLabelTextMapper: MailLabelTextMapper
) {

    internal fun newStateFrom(operation: MailboxOperation.AffectingActionMessage): Effect<ActionResult> {
        val actionResult = when (operation) {
            is MailboxEvent.MoveToConfirmed.Category -> {
                val destination = mailLabelTextMapper.mapToString(operation.label)
                UndoableActionResult(
                    TextUiModel(LabelR.string.move_to_category_action_message, destination)
                )
            }

            is MailboxEvent.MoveToConfirmed ->
                bulkActionMessageFactory.moveResult(operation.label, operation.itemCount, operation.viewMode)

            is MailboxEvent.LabelAsConfirmed -> {
                // Do not show a snackbar on labeling with no archive
                if (!operation.alsoArchived) return Effect.empty()

                bulkActionMessageFactory.moveResult(
                    destination = MailLabelText(R.string.label_title_archive),
                    itemCount = operation.itemCount,
                    viewMode = operation.viewMode
                )
            }

            is MailboxEvent.DeleteConfirmed ->
                bulkActionMessageFactory.deleteResult(operation.numAffectedMessages, operation.viewMode)

            is MailboxViewAction.SwipeArchiveAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_archive_message)
            )

            is MailboxViewAction.SwipeSpamAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_spam_message)
            )

            is MailboxViewAction.SwipeTrashAction -> UndoableActionResult(
                TextUiModel(R.string.mailbox_action_trash_message)
            )

            is MailboxEvent.MaxSelectionLimitReached -> bulkActionMessageFactory.maxSelectionReachedResult()
        }
        return Effect.of(actionResult)
    }
}
