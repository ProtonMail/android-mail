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

import ch.protonmail.android.mailmessage.domain.model.Message

/**
 * A search result the user opened before and has not dismissed.
 *
 * The Rust layer resolves the entry back to its live message before returning it, so [message] is
 * already the same shape the rest of the app renders — there is no separate "message is gone" case to
 * handle here.
 *
 * @param searchQuery the query that surfaced the item, kept so reopening it can highlight the same
 * terms. Null when the recorded query is no longer known.
 * @param lastOpenedAt seconds since the epoch, like mailbox item times.
 */
data class RecentFoundItem(
    val message: Message,
    val searchQuery: String?,
    val lastOpenedAt: Long
)
