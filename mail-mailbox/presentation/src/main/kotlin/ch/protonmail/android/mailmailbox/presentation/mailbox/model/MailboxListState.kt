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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.AutoDeleteBannerUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import me.proton.core.domain.entity.UserId

sealed interface MailboxListState {

    sealed interface Data : MailboxListState {

        sealed interface ClearState {
            data object Hidden : ClearState
            sealed interface Visible : ClearState {
                data class Button(val text: TextUiModel) : Visible
                data object Banner : Visible
            }
        }

        sealed interface AutoDeleteBannerState {
            data object Hidden : AutoDeleteBannerState
            data class Visible(
                val uiModel: AutoDeleteBannerUiModel
            ) : AutoDeleteBannerState
        }

        val currentMailLabel: MailLabel
        val swipingEnabled: Boolean
        val swipeActions: SwipeActionsUiModel?
        val clearState: ClearState
        val autoDeleteBannerState: AutoDeleteBannerState
        val searchState: MailboxSearchState

        data class ViewMode(
            override val currentMailLabel: MailLabel,
            override val swipingEnabled: Boolean,
            override val swipeActions: SwipeActionsUiModel?,
            override val clearState: ClearState,
            override val autoDeleteBannerState: AutoDeleteBannerState,
            override val searchState: MailboxSearchState,
            val openItemEffect: Effect<OpenMailboxItemRequest>,
            val scrollToMailboxTop: Effect<MailLabelId>,
            val offlineEffect: Effect<Unit>,
            val refreshErrorEffect: Effect<Unit>,
            val refreshRequested: Boolean
        ) : Data {
            fun isInInboxLabel() = currentMailLabel.id == MailLabelId.System.Inbox
        }

        data class SelectionMode(
            override val currentMailLabel: MailLabel,
            override val swipingEnabled: Boolean,
            override val swipeActions: SwipeActionsUiModel?,
            override val clearState: ClearState,
            override val autoDeleteBannerState: AutoDeleteBannerState,
            override val searchState: MailboxSearchState,
            val selectedMailboxItems: Set<SelectedMailboxItem>
        ) : Data {

            data class SelectedMailboxItem(
                val userId: UserId,
                val id: String,
                val isRead: Boolean,
                val isStarred: Boolean
            )
        }
    }

    data object Loading : MailboxListState
}
