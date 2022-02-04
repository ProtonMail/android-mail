/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.PagingState
import androidx.room.RoomDatabase
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.getAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.getRefreshPageKey
import ch.protonmail.android.mailpagination.presentation.paging.InvalidationTrackerPagingSource
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.usecase.GetMultiUserMailboxItems
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import java.io.IOException
import kotlin.math.max

@AssistedFactory
interface MailboxItemPagingSourceFactory {
    fun create(
        userIds: List<UserId>,
        location: SidebarLocation,
        type: MailboxItemType,
    ): MailboxItemPagingSource
}

class MailboxItemPagingSource @AssistedInject constructor(
    roomDatabase: RoomDatabase,
    private val getMailboxItems: GetMultiUserMailboxItems,
    @Assisted private val userIds: List<UserId>,
    @Assisted private val location: SidebarLocation,
    @Assisted private val type: MailboxItemType,
) : InvalidationTrackerPagingSource<MailboxPageKey, MailboxItem>(
    db = roomDatabase,
    tables = GetMultiUserMailboxItems.getInvolvedTables(type)
) {

    private val initialPageKey = MailboxPageKey(
        userIds = userIds,
        pageKey = PageKey(filter = PageFilter(labelId = location.labelId))
    )

    override suspend fun loadPage(
        params: LoadParams<MailboxPageKey>,
    ): LoadResult<MailboxPageKey, MailboxItem> {
        try {
            val key = params.key ?: initialPageKey
            val size = max(key.pageKey.size, params.loadSize)

            val items = getMailboxItems(type, key.copy(pageKey = key.pageKey.copy(size = size)))
            Timber.d("loadItems: ${items.size}/$size -> ${key.pageKey}")

            val adjacentKeys = items.getAdjacentPageKeys(key.pageKey, initialPageKey.pageKey.size)
            val prev = key.copy(pageKey = adjacentKeys.prev)
            val next = key.copy(pageKey = adjacentKeys.next)
            return LoadResult.Page(
                data = items,
                prevKey = prev.takeIf { items.isNotEmpty() },
                nextKey = next.takeIf { items.isNotEmpty() }
            )
        } catch (e: IOException) {
            Timber.d(e)
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(
        state: PagingState<MailboxPageKey, MailboxItem>,
    ): MailboxPageKey? {
        val items = state.pages.flatMap { it.data }.takeIfNotEmpty() ?: return null
        val key = items.getRefreshPageKey(initialPageKey.pageKey)
        return initialPageKey.copy(
            pageKey = key.copy(size = max(key.size, state.config.initialLoadSize)),
        )
    }
}
