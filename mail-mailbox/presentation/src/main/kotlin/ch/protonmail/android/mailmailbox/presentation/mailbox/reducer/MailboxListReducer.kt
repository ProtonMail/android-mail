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
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class MailboxListReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: MailboxListState,
        operation: MailboxOperation.AffectingMailboxList
    ): MailboxListState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> {
                val currentMailLabel = operation.selectedLabel
                when (currentState) {
                    is MailboxListState.Loading -> MailboxListState.Data(
                        currentMailLabel,
                        openItemEffect = Effect.empty(),
                        scrollToMailboxTop = Effect.empty()
                    )
                    is MailboxListState.Data -> currentState.copy(
                        currentMailLabel = currentMailLabel
                    )
                }
            }
            is MailboxEvent.NewLabelSelected -> {
                val currentMailLabel = operation.selectedLabel
                when (currentState) {
                    is MailboxListState.Loading -> MailboxListState.Data(
                        currentMailLabel,
                        openItemEffect = Effect.empty(),
                        scrollToMailboxTop = Effect.empty()
                    )
                    is MailboxListState.Data -> currentState.copy(
                        currentMailLabel = currentMailLabel,
                        scrollToMailboxTop = Effect.of(currentMailLabel.id)
                    )
                }
            }
            is MailboxEvent.ItemDetailsOpenedInViewMode -> {
                val request = when (operation.preferredViewMode) {
                    ViewMode.ConversationGrouping -> {
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(operation.item.conversationId.id),
                            itemType = MailboxItemType.Conversation
                        )
                    }
                    ViewMode.NoConversationGrouping -> {
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(operation.item.id),
                            itemType = operation.item.type
                        )
                    }
                }
                when (currentState) {
                    is MailboxListState.Loading -> currentState
                    is MailboxListState.Data -> currentState.copy(openItemEffect = Effect.of(request))
                }
            }
        }
    }
}
