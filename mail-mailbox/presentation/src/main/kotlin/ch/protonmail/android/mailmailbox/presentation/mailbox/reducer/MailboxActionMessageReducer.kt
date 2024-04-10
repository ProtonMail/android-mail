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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.Companion.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.Companion.UndoableActionResult
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class MailboxActionMessageReducer @Inject constructor() {

    internal fun newStateFrom(operation: MailboxOperation.AffectingActionMessage): Effect<ActionResult> {
        val actionResult = when (operation) {
            is MailboxEvent.Trash ->
                UndoableActionResult(
                    TextUiModel(R.plurals.mailbox_action_trash, operation.numAffectedMessages)
                )


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
        }
        return Effect.of(actionResult)
    }
}
