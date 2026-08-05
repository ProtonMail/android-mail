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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchResultUiModel
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel

data class ContentSearchActions(
    val onClose: () -> Unit,
    val onSuggestionSelected: (String) -> Unit,
    val onClearQuery: () -> Unit,
    val onToggleIncludeSpam: () -> Unit,
    val onToggleIncludeTrash: () -> Unit,
    val onItemClicked: (ContentSearchResultUiModel) -> Unit,
    val onStarClicked: (MailboxItemUiModel) -> Unit,
    val onItemLongClicked: (MailboxItemUiModel) -> Unit,
    val onAvatarImageLoadRequested: (MailboxItemUiModel) -> Unit,
    val onAvatarImageLoadFailed: (MailboxItemUiModel) -> Unit,
    val onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
    val onExitSelectionMode: () -> Unit,
    val onMarkRead: () -> Unit,
    val onMarkUnread: () -> Unit,
    val onStarSelection: () -> Unit,
    val onUnStarSelection: () -> Unit,
    val onTrash: () -> Unit,
    val onArchive: () -> Unit,
    val onSpam: () -> Unit,
    val onMoveToInbox: () -> Unit,
    val onDelete: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onDeleteDialogDismissed: () -> Unit,
    val onRequestMoveTo: () -> Unit,
    val onRequestLabelAs: () -> Unit,
    val onRequestMore: () -> Unit,
    val onDismissBottomSheet: () -> Unit,
    val onMoveToCompleted: (destination: MailLabelText, itemCount: Int) -> Unit,
    val onLabelAsCompleted: (archived: Boolean, itemCount: Int) -> Unit
) {

    companion object {

        val Empty = ContentSearchActions(
            onClose = {},
            onSuggestionSelected = {},
            onClearQuery = {},
            onToggleIncludeSpam = {},
            onToggleIncludeTrash = {},
            onItemClicked = {},
            onStarClicked = {},
            onItemLongClicked = {},
            onAvatarImageLoadRequested = {},
            onAvatarImageLoadFailed = {},
            onAttachmentClicked = {},
            onExitSelectionMode = {},
            onMarkRead = {},
            onMarkUnread = {},
            onStarSelection = {},
            onUnStarSelection = {},
            onTrash = {},
            onArchive = {},
            onSpam = {},
            onMoveToInbox = {},
            onDelete = {},
            onDeleteConfirmed = {},
            onDeleteDialogDismissed = {},
            onRequestMoveTo = {},
            onRequestLabelAs = {},
            onRequestMore = {},
            onDismissBottomSheet = {},
            onMoveToCompleted = { _, _ -> },
            onLabelAsCompleted = { _, _ -> }
        )
    }
}
