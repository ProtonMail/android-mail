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

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelInProgressSignal
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class MailboxPagerFactory @Inject constructor(
    private val pagingSourceFactory: MailboxItemPagingSourceFactory,
    private val remoteMediatorFactory: MailboxItemRemoteMediatorFactory
) {

    @Suppress("LongParameterList")
    fun create(
        userIds: List<UserId>,
        selectedMailLabelId: MailLabelId,
        filterUnread: Boolean,
        type: MailboxItemType,
        searchQuery: String,
        emptyLabelInProgressSignal: EmptyLabelInProgressSignal
    ): Pager<MailboxPageKey, MailboxItem> {
        val mailboxPageKey = buildPageKey(filterUnread, selectedMailLabelId, userIds, searchQuery)
        return Pager(
            config = PagingConfig(PageKey.defaultPageSize),
            remoteMediator = remoteMediatorFactory.create(mailboxPageKey, type, emptyLabelInProgressSignal),
            pagingSourceFactory = { pagingSourceFactory.create(mailboxPageKey, type) }
        )
    }

    private fun buildPageKey(
        filterUnread: Boolean,
        selectedMailLabelId: MailLabelId,
        userIds: List<UserId>,
        searchQuery: String
    ) = MailboxPageKey(
        userIds = userIds,
        pageKey = PageKey(
            PageFilter(
                keyword = searchQuery,
                labelId = selectedMailLabelId.labelId,
                read = if (filterUnread) ReadStatus.Unread else ReadStatus.All
            )
        )
    )
}
