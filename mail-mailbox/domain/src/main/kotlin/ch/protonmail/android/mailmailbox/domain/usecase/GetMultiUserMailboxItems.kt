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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.Either
import arrow.core.sequence
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import me.proton.core.util.kotlin.mapAsync
import javax.inject.Inject

/**
 * Get MailboxItems for multi-users and merge lists.
 *
 * @see GetMailboxItems
 */
class GetMultiUserMailboxItems @Inject constructor(
    private val getMailboxItems: GetMailboxItems
) {

    suspend operator fun invoke(type: MailboxItemType, pageKey: MailboxPageKey): Either<DataError, List<MailboxItem>> {
        return pageKey.userIds
            .mapAsync { getMailboxItems(userId = it, type = type, pageKey = pageKey.pageKey) }
            .sequence()
            .map { list -> list.mergeBy(pageKey) }
    }

    private fun List<List<MailboxItem>>.mergeBy(pageKey: MailboxPageKey) = flatten().let { list ->
        when (pageKey.pageKey.orderDirection) {
            OrderDirection.Ascending -> list.sortedBy { pageKey.selector(it) }
            OrderDirection.Descending -> list.sortedByDescending { pageKey.selector(it) }
        }.take(pageKey.pageKey.size) // Only take pageKey.size items to match expected size.
    }

    private fun MailboxPageKey.selector(item: MailboxItem) = when (pageKey.orderBy) {
        OrderBy.Time -> "${item.time}-${item.order}"
    }

    companion object {
        /**
         * Return DB Tables involved to invalidate [invoke] according [type].
         */
        fun getInvolvedTables(type: MailboxItemType) = GetMailboxItems.getInvolvedTables(type)
    }
}
