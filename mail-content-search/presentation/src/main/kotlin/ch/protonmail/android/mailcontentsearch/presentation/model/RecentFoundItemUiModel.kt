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

import androidx.compose.runtime.Immutable
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId

/**
 * A previously opened search result, carried as a full mailbox item so the row renders exactly like a
 * mailbox or search-result row (avatar, read state, star, labels).
 *
 * @param searchQuery the query the item was found by, forwarded on open so the detail screen
 * highlights the same terms. Null when no query was recorded.
 */
@Immutable
data class RecentFoundItemUiModel(
    val item: MailboxItemUiModel,
    val searchQuery: String?
) {

    val messageId: MessageId get() = MessageId(item.id)
}
