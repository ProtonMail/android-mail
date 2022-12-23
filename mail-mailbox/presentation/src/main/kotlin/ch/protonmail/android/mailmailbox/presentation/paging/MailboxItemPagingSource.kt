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

import androidx.paging.PagingState
import androidx.room.RoomDatabase
import arrow.core.getOrHandle
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.usecase.GetMultiUserMailboxItems
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.getRefreshPageKey
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.mailpagination.presentation.paging.InvalidationTrackerPagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import kotlin.math.max

@AssistedFactory
interface MailboxItemPagingSourceFactory {

    fun create(
        userIds: List<UserId>,
        selectedMailLabelId: MailLabelId,
        filterUnread: Boolean,
        type: MailboxItemType
    ): MailboxItemPagingSource
}

class MailboxItemPagingSource @AssistedInject constructor(
    roomDatabase: RoomDatabase,
    private val getMailboxItems: GetMultiUserMailboxItems,
    private val getAdjacentPageKeys: GetAdjacentPageKeys,
    @Assisted private val userIds: List<UserId>,
    @Assisted private val selectedMailLabelId: MailLabelId,
    @Assisted private val filterUnread: Boolean,
    @Assisted private val type: MailboxItemType
) : InvalidationTrackerPagingSource<MailboxPageKey, MailboxItem>(
    db = roomDatabase,
    tables = GetMultiUserMailboxItems.getInvolvedTables(type)
) {

    private val initialPageKey = MailboxPageKey(
        userIds = userIds,
        pageKey = PageKey(
            filter = PageFilter(
                labelId = selectedMailLabelId.labelId,
                read = if (filterUnread) ReadStatus.Unread else ReadStatus.All
            )
        )
    )

    override suspend fun loadPage(
        params: LoadParams<MailboxPageKey>
    ): LoadResult<MailboxPageKey, MailboxItem> {
        val key = params.key ?: initialPageKey
        val size = max(key.pageKey.size, params.loadSize)

        val items = getMailboxItems(type, key.copy(pageKey = key.pageKey.copy(size = size)))
            .getOrHandle { return LoadResult.Error(RuntimeException(it.toString())) }
        Timber.d("loadItems: ${items.size}/$size -> ${key.pageKey}")

        val adjacentKeys = getAdjacentPageKeys(items, key.pageKey, initialPageKey.pageKey.size)
        val prev = key.copy(pageKey = adjacentKeys.prev)
        val next = key.copy(pageKey = adjacentKeys.next)
        return LoadResult.Page(
            data = items,
            prevKey = prev.takeIf { items.isNotEmpty() },
            nextKey = next.takeIf { items.isNotEmpty() }
        )
    }

    override fun getRefreshKey(
        state: PagingState<MailboxPageKey, MailboxItem>
    ): MailboxPageKey? {
        val items = state.pages.flatMap { it.data }.takeIfNotEmpty() ?: return null
        val key = items.getRefreshPageKey(initialPageKey.pageKey)
        return initialPageKey.copy(
            pageKey = key.copy(size = max(key.size, state.config.initialLoadSize))
        )
    }
}
