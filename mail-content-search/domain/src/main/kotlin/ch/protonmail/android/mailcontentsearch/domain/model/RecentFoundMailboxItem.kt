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

package ch.protonmail.android.mailcontentsearch.domain.model

import ch.protonmail.android.mailmailbox.domain.model.MailboxItem

/**
 * A [RecentFoundItem] resolved back to its live message, so the "previously found" rows can render as
 * ordinary mailbox items (avatar, read state, star, labels) instead of from the recents table's own
 * denormalised copy of the subject and sender.
 *
 * @param searchQuery the query the item was found by, carried over from the recents entry so opening
 * it can highlight the same terms. Null when no query was recorded.
 */
data class RecentFoundMailboxItem(
    val item: MailboxItem,
    val searchQuery: String?
)
