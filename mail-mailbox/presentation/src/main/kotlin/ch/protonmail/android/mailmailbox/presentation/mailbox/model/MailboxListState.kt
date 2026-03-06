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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent

sealed interface MailboxListState {

    sealed interface Data : MailboxListState {

        val currentMailLabel: MailLabel
        val swipeActions: SwipeActionsUiModel?
        val searchState: MailboxSearchState
        val shouldShowFab: Boolean
        val avatarImagesUiModel: AvatarImagesUiModel
        val paginatorInvalidationEffect: Effect<PageInvalidationEvent>
        val refreshOngoing: Boolean
        val loadingBarState: LoadingBarUiState

        data class ViewMode(
            override val currentMailLabel: MailLabel,
            override val swipeActions: SwipeActionsUiModel?,
            override val searchState: MailboxSearchState,
            override val shouldShowFab: Boolean,
            override val avatarImagesUiModel: AvatarImagesUiModel,
            override val paginatorInvalidationEffect: Effect<PageInvalidationEvent> = Effect.empty(),
            override val refreshOngoing: Boolean,
            override val loadingBarState: LoadingBarUiState,
            val openItemEffect: Effect<OpenMailboxItemRequest>,
            val scrollToMailboxTop: Effect<MailLabelId>,
            val refreshErrorEffect: Effect<Unit>,
            val displayAttachment: Effect<OpenAttachmentIntentValues> = Effect.empty(),
            val displayAttachmentError: Effect<TextUiModel> = Effect.empty(),
            val attachmentOpeningStarted: Effect<TextUiModel> = Effect.empty()
        ) : Data {

            fun isInInboxLabel() = (currentMailLabel as? MailLabel.System)?.systemLabelId == SystemLabelId.Inbox
        }

        data class SelectionMode(
            override val currentMailLabel: MailLabel,
            override val swipeActions: SwipeActionsUiModel?,
            override val searchState: MailboxSearchState,
            override val shouldShowFab: Boolean,
            override val avatarImagesUiModel: AvatarImagesUiModel,
            override val paginatorInvalidationEffect: Effect<PageInvalidationEvent> = Effect.empty(),
            override val refreshOngoing: Boolean,
            override val loadingBarState: LoadingBarUiState,
            val selectedMailboxItems: Set<SelectedMailboxItem>,
            val areAllItemsSelected: Boolean
        ) : Data {

            data class SelectedMailboxItem(
                val id: String,
                val isRead: Boolean,
                val isStarred: Boolean
            )
        }
    }

    data object Loading : MailboxListState
    data object CouldNotLoadUserSession : MailboxListState

    companion object {

        const val maxItemSelectionLimit = 100
    }
}

fun MailboxListState.hasClearableOperations() = this is MailboxListState.Data.ViewMode && !this.searchState.isInSearch()

fun MailboxListState.getHighlightText(): String {
    return if (this is MailboxListState.Data && this.searchState.isInSearch()) {
        this.searchState.searchQuery
    } else {
        ""
    }
}
