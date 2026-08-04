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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundMailboxItem
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Reads the previously found items and maps each one's already-resolved message to a mailbox row.
 *
 * The Rust layer resolves the entry back to its live message before returning it, so there is no
 * separate repository round trip here, and no "message is gone" case to drop.
 */
class GetRecentFoundMailboxItems @Inject constructor(
    private val getRecentFoundItems: GetRecentFoundItems,
    private val messageMailboxItemMapper: MessageMailboxItemMapper
) {

    suspend operator fun invoke(
        userId: UserId,
        limit: Int = GetRecentFoundItems.DefaultLimit
    ): Either<DataError, List<RecentFoundMailboxItem>> = getRecentFoundItems(userId, limit).map { recents ->
        recents.map { recent ->
            RecentFoundMailboxItem(
                item = messageMailboxItemMapper.toMailboxItem(recent.message),
                searchQuery = recent.searchQuery
            )
        }
    }
}
