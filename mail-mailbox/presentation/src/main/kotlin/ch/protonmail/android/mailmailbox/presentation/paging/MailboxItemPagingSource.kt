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
import arrow.core.getOrElse
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.usecase.GetMultiUserMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.IsMultiUserLocalPageValid
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.getRefreshPageKey
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.presentation.paging.InvalidationTrackerPagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import kotlin.math.max

@AssistedFactory
interface MailboxItemPagingSourceFactory {

    fun create(mailboxPageKey: MailboxPageKey, type: MailboxItemType): MailboxItemPagingSource
}

class MailboxItemPagingSource @AssistedInject constructor(
    roomDatabase: RoomDatabase,
    private val getMailboxItems: GetMultiUserMailboxItems,
    private val getAdjacentPageKeys: GetAdjacentPageKeys,
    private val isMultiUserLocalPageValid: IsMultiUserLocalPageValid,
    @Assisted private val mailboxPageKey: MailboxPageKey,
    @Assisted private val type: MailboxItemType
) : InvalidationTrackerPagingSource<MailboxPageKey, MailboxItem>(
    db = roomDatabase,
    tables = GetMultiUserMailboxItems.getInvolvedTables(type)
) {

    override suspend fun loadPage(params: LoadParams<MailboxPageKey>): LoadResult<MailboxPageKey, MailboxItem> {
        val key = params.key ?: mailboxPageKey
        val size = max(key.pageKey.size, params.loadSize)
        val pageKey = key.pageKey.copy(size = size)

        val items = getMailboxItems(type, key.copy(pageKey = pageKey)).getOrElse {
            Timber.e("Paging: loadItems: Error $it")
            return LoadResult.Page(emptyList(), null, null)
        }
        Timber.d("Paging: loadItems: ${items.size}/$size (${params.javaClass.simpleName})-> $pageKey")

        val adjacentKeys = getAdjacentPageKeys(items, pageKey, mailboxPageKey.pageKey.size)
        val prev = key.copy(pageKey = adjacentKeys.prev)
        val next = key.copy(pageKey = adjacentKeys.next)
        return LoadResult.Page(
            data = items,
            prevKey = prev.doNotTakeWhenMediatorShouldPrepend(params, items)?.doNotTakeWhenEqualToNext(next),
            nextKey = next.doNotTakeWhenMediatorShouldAppend(params, items)
        )
    }

    override fun getRefreshKey(state: PagingState<MailboxPageKey, MailboxItem>): MailboxPageKey? =
        getAllPagesRefreshKey(state)

    /**
     * When refreshing we get a key that represent all pages loaded so far.
     * This is needed due to the complexity of our key not allowing to precisely determine the key
     * to the currently displayed page. (All other solutions tried to achieve that caused some "jumping"
     * in the list)
     */
    private fun getAllPagesRefreshKey(state: PagingState<MailboxPageKey, MailboxItem>): MailboxPageKey? {
        Timber.d("Paging: getRefreshKey: ${state.pages.size} pages")
        val items = state.pages.flatMap { it.data }.takeIfNotEmpty() ?: return null
        Timber.d("Paging: getRefreshKey: ${items.size} items")
        val key = items.getRefreshPageKey(mailboxPageKey.pageKey)
        if (key.isRefreshingOneItem()) {
            // Solve the issue detailed in MAILANDR-854
            return mailboxPageKey.copy(
                pageKey = key.copy(filter = key.filter.copy(maxTime = Long.MAX_VALUE))
            )
        }
        return mailboxPageKey.copy(pageKey = key)
    }

    /*
     * A PageKey is considered to be refreshing one item only if the min time and max time are the same
     */
    private fun PageKey.isRefreshingOneItem() = this.filter.minTime == this.filter.maxTime

    /*
     * Use this MailboxPageKey when it is NOT Append OR items are NOT empty.
     * (takeIf { params !is LoadParams.Append || items.isNotEmpty() })
     *
     * This is used to trigger the loading from the remote mediator, which happens when a NULL key is returned.
     * We only want to return a null key for append operation when:
     * - we are doing an APPEND operation AND
     * - the list of items found locally (to be appended) is empty.
     *
     * In all other cases, this key will be used so that the mediator will NOT be triggered
     * (eg. when we are doing an PREPEND op we never want to trigger a network call for appending)
     */
    private suspend fun MailboxPageKey.doNotTakeWhenMediatorShouldAppend(
        params: LoadParams<MailboxPageKey>,
        items: List<MailboxItem>
    ) = takeIf { isMultiUserLocalPageValid(type, this) }
        .takeUnless { params is LoadParams.Append && items.isEmpty() }

    /*
     * Use this MailboxPageKey when it is NOT Prepend OR items are NOT empty.
     * (takeIf { params !is LoadParams.Prepend || items.isNotEmpty() })
     *
     * This is used to trigger the loading from the remote mediator, which happens when a NULL key is returned.
     * We only want to return a null key for prepend operation when:
     * - we are doing a PREPEND operation AND
     * - the list of items found locally (to be prepended) is empty.
     *
     * In all other cases, this key will be used so that the mediator will NOT be triggered
     * (eg. when we are doing an APPEND op we never want to trigger a network call for prepending)
     */
    private suspend fun MailboxPageKey.doNotTakeWhenMediatorShouldPrepend(
        params: LoadParams<MailboxPageKey>,
        items: List<MailboxItem>
    ) = takeIf { isMultiUserLocalPageValid(type, this) }
        .takeUnless { params is LoadParams.Prepend && items.isEmpty() }

    /*
     * Use this MailboxPageKey when it is NOT equal as the next key (given as param)
     *
     * This is used to prevent two "open" keys (-INF -> +INF) to be loaded at once,
     * which in some cases (MAILANDR-1419) would result in the same item being returned twice
     * consequentially causing a crash when trying to render it in the Mailbox
     * (Compose LazyList unique id violated)
     */
    private fun MailboxPageKey.doNotTakeWhenEqualToNext(nextKey: MailboxPageKey) = takeUnless { this == nextKey }
}
