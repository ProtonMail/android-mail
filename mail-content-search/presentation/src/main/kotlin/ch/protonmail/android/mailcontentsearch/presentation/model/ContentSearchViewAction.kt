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

package ch.protonmail.android.mailcontentsearch.presentation.model

import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel

sealed interface ContentSearchViewAction {

    data object Search : ContentSearchViewAction

    data class SuggestionSelected(val query: String) : ContentSearchViewAction

    data object ClearQuery : ContentSearchViewAction

    data object ToggleIncludeSpam : ContentSearchViewAction

    data object ToggleIncludeTrash : ContentSearchViewAction

    data class ResultsLoaded(val count: Int) : ContentSearchViewAction

    data class StarAction(val itemId: String, val isStarred: Boolean) : ContentSearchViewAction

    data class AvatarImageLoadRequested(val item: MailboxItemUiModel) : ContentSearchViewAction

    data class AvatarImageLoadFailed(val item: MailboxItemUiModel) : ContentSearchViewAction

    data class RequestAttachment(val attachmentId: AttachmentIdUiModel) : ContentSearchViewAction

    // Selection mode
    data class ItemLongClicked(val item: MailboxItemUiModel) : ContentSearchViewAction

    data class ToggleItemSelection(val item: MailboxItemUiModel) : ContentSearchViewAction

    data object ExitSelectionMode : ContentSearchViewAction

    data class ItemsRemovedFromSelection(val itemIds: List<String>) : ContentSearchViewAction

    // Bottom toolbar actions on the current selection
    data object MarkAsRead : ContentSearchViewAction

    data object MarkAsUnread : ContentSearchViewAction

    data object Star : ContentSearchViewAction

    data object UnStar : ContentSearchViewAction

    data object Trash : ContentSearchViewAction

    data object MoveToArchive : ContentSearchViewAction

    data object MoveToSpam : ContentSearchViewAction

    data object MoveToInbox : ContentSearchViewAction

    data object Delete : ContentSearchViewAction

    data object DeleteConfirmed : ContentSearchViewAction

    data object DeleteDialogDismissed : ContentSearchViewAction

    // Bottom sheets (move-to / label-as / more overflow)
    data object RequestMoveToBottomSheet : ContentSearchViewAction

    data object RequestLabelAsBottomSheet : ContentSearchViewAction

    data object RequestMoreActionsBottomSheet : ContentSearchViewAction

    data object DismissBottomSheet : ContentSearchViewAction

    data class MoveToCompleted(val destination: MailLabelText, val itemCount: Int) : ContentSearchViewAction

    data class LabelAsCompleted(val archived: Boolean, val itemCount: Int) : ContentSearchViewAction
}
