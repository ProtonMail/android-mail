/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import javax.inject.Inject

/**
 * Builds the confirmation-snackbar [ch.protonmail.android.mailcommon.presentation.model.ActionResult]s for
 * bulk move/delete actions. Shared so any selection-capable screen (mailbox, content search) phrases these
 * snackbars identically — including the [ViewMode]-driven message-vs-conversation pluralisation and the
 * undoable-move / definitive-delete distinction — instead of re-deriving them per screen.
 */
class BulkActionMessageFactory @Inject constructor(
    private val mailLabelTextMapper: MailLabelTextMapper
) {

    /** An undoable "N moved to {destination}" snackbar, pluralised by [viewMode]. */
    fun moveResult(
        destination: MailLabelText,
        itemCount: Int,
        viewMode: ViewMode,
        conversationRes: Int = R.plurals.mailbox_action_move_conversation,
        messageRes: Int = R.plurals.mailbox_action_move_message
    ): UndoableActionResult {
        val pluralsRes = when (viewMode) {
            ViewMode.ConversationGrouping -> conversationRes
            ViewMode.NoConversationGrouping -> messageRes
        }
        return UndoableActionResult(
            TextUiModel.PluralisedText(
                value = pluralsRes,
                count = itemCount,
                formatArgs = listOf(mailLabelTextMapper.mapToString(destination))
            )
        )
    }

    /**
     * Refuses a selection that would exceed
     * [ch.protonmail.android.mailcommon.presentation.model.SelectionState.MaxItemSelectionLimit].
     */
    fun maxSelectionReachedResult(): DefinitiveActionResult =
        DefinitiveActionResult(TextUiModel(R.string.mailbox_action_maximum_selection_reached))

    /** A definitive "N deleted" snackbar (delete is permanent, so it is not undoable), pluralised by [viewMode]. */
    fun deleteResult(itemCount: Int, viewMode: ViewMode): DefinitiveActionResult {
        val pluralsRes = when (viewMode) {
            ViewMode.ConversationGrouping -> R.plurals.mailbox_action_delete_conversation
            ViewMode.NoConversationGrouping -> R.plurals.mailbox_action_delete_message
        }
        return DefinitiveActionResult(TextUiModel(pluralsRes, itemCount))
    }
}
