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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.paging.Pager
import androidx.paging.PagingConfig
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MailboxPagerFactory @Inject constructor(
    private val pagingSourceFactory: MailboxItemPagingSourceFactory
) {

    fun create(
        userIds: List<UserId>,
        selectedMailLabelId: MailLabelId,
        filterUnread: Boolean,
        type: MailboxItemType,
    ) = Pager(
        config = PagingConfig(PageKey.defaultPageSize),
        initialKey = buildPageKey(filterUnread, selectedMailLabelId, userIds)
    ) {
        pagingSourceFactory.create(
            userIds = userIds,
            selectedMailLabelId = selectedMailLabelId,
            filterUnread = filterUnread,
            type = type
        )
    }

    private fun buildPageKey(
        filterUnread: Boolean,
        selectedMailLabelId: MailLabelId,
        userIds: List<UserId>
    ) = MailboxPageKey(
        userIds = userIds,
        pageKey = PageKey(
            PageFilter(
                labelId = selectedMailLabelId.labelId,
                read = if (filterUnread) ReadStatus.Unread else ReadStatus.All
            )
        )
    )
}
