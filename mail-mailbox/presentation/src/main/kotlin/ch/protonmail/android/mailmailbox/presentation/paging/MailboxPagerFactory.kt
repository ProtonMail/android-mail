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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelIdWithCategory
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MailboxPagerFactory @Inject constructor(
    private val pagingSourceFactory: MailboxItemPagingSourceFactory
) {

    @Suppress("LongParameterList")
    fun create(
        userId: UserId,
        selectedLabelWithCategory: MailLabelIdWithCategory,
        type: MailboxItemType,
        searchQuery: String
    ): Pager<MailboxPageKey, MailboxItem> {

        val mailboxPageKey = if (searchQuery.isNotEmpty()) {
            buildSearchPageKey(
                userId = userId,
                searchQuery = searchQuery
            )
        } else {
            buildDefaultPageKey(
                selectedMailLabelId = selectedLabelWithCategory.mailLabelId,
                userId = userId,
                categoryLabelId = selectedLabelWithCategory.categoryLabelId
            )
        }
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = INITIAL_LOAD_SIZE),
            pagingSourceFactory = { pagingSourceFactory.create(mailboxPageKey, type) }
        )
    }

    private fun buildDefaultPageKey(
        selectedMailLabelId: MailLabelId,
        userId: UserId,
        categoryLabelId: CategoryLabelId?
    ): MailboxPageKey {
        val pageKey = PageKey.DefaultPageKey(
            labelId = selectedMailLabelId.labelId,
            categoryLabelId = categoryLabelId
        )

        return MailboxPageKey(
            userId = userId,
            pageKey = pageKey
        )
    }

    private fun buildSearchPageKey(userId: UserId, searchQuery: String): MailboxPageKey {
        val pageKey =
            PageKey.PageKeyForSearch(
                keyword = searchQuery
            )

        return MailboxPageKey(
            userId = userId,
            pageKey = pageKey
        )
    }

    companion object {

        private const val DEFAULT_PAGE_SIZE = 50
        private const val INITIAL_LOAD_SIZE = 1
    }
}
