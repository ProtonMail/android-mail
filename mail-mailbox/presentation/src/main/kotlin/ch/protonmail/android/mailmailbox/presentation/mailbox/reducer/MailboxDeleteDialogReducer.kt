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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation.AffectingDeleteDialog
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class MailboxDeleteDialogReducer @Inject constructor() {

    internal fun newStateFrom(operation: AffectingDeleteDialog) = when (operation) {
        is MailboxEvent.Delete -> newStateFromDelete(operation)
        is MailboxEvent.DeleteConfirmed -> DeleteDialogState.Hidden
        is MailboxViewAction.DeleteDialogDismissed -> DeleteDialogState.Hidden
    }

    private fun newStateFromDelete(operation: MailboxEvent.Delete): DeleteDialogState {
        val titleRes = when (operation.viewMode) {
            ViewMode.ConversationGrouping -> R.plurals.mailbox_action_delete_conversation_dialog_title
            ViewMode.NoConversationGrouping -> R.plurals.mailbox_action_delete_message_dialog_title
        }
        val messageRes = when (operation.viewMode) {
            ViewMode.ConversationGrouping -> R.plurals.mailbox_action_delete_conversation_dialog_message
            ViewMode.NoConversationGrouping -> R.plurals.mailbox_action_delete_message_dialog_message
        }
        val titleText = TextUiModel.PluralisedText(value = titleRes, count = operation.numAffectedMessages)
        val messageText = TextUiModel.PluralisedText(value = messageRes, count = operation.numAffectedMessages)
        return DeleteDialogState.Shown(titleText, messageText)
    }

}
