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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import javax.inject.Inject

class MessageDeleteDialogReducer @Inject constructor() {

    internal fun newStateFrom(operation: MessageDetailOperation.AffectingDeleteDialog) = when (operation) {
        MessageViewAction.DeleteRequested -> newStateFromDeleteRequested()
        MessageDetailEvent.ErrorDeletingMessage,
        MessageDetailEvent.ErrorDeletingNoApplicableFolder,
        MessageViewAction.DeleteConfirmed,
        MessageViewAction.DeleteDialogDismissed -> DeleteDialogState.Hidden
    }

    private fun newStateFromDeleteRequested(): DeleteDialogState {
        return DeleteDialogState.Shown(
            title = TextUiModel.TextRes(R.string.message_delete_dialog_title),
            message = TextUiModel.TextRes(R.string.message_delete_dialog_message)
        )
    }
}
