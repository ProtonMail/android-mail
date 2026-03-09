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

package ch.protonmail.android.mailmessage.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailpagination.data.mapper.toPaginationError
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import timber.log.Timber
import uniffi.mail_uniffi.IncludeSwitch
import uniffi.mail_uniffi.PaginatorSearchOptions
import uniffi.mail_uniffi.SearchScroller
import uniffi.mail_uniffi.SearchScrollerCursorResult
import uniffi.mail_uniffi.SearchScrollerFetchMoreResult
import uniffi.mail_uniffi.SearchScrollerGetItemsResult
import uniffi.mail_uniffi.SearchScrollerSupportsIncludeFilterResult

class SearchMessagePaginatorWrapper(
    private val rustPaginator: SearchScroller
) : MessagePaginatorWrapper {

    override suspend fun supportsIncludeFilter() = when (val result = rustPaginator.supportsIncludeFilter()) {
        is SearchScrollerSupportsIncludeFilterResult.Error -> {
            Timber.w("search-paginator: failed to define supportsIncludeFilter: $result")
            false
        }

        is SearchScrollerSupportsIncludeFilterResult.Ok -> result.v1
    }

    override suspend fun nextPage(): Either<PaginationError, Unit> = when (val result = rustPaginator.fetchMore()) {
        is SearchScrollerFetchMoreResult.Error -> result.v1.toPaginationError().left()
        is SearchScrollerFetchMoreResult.Ok -> Unit.right()
    }

    override suspend fun reload(): Either<PaginationError, Unit> = when (val result = rustPaginator.getItems()) {
        is SearchScrollerGetItemsResult.Error -> result.v1.toPaginationError().left()
        is SearchScrollerGetItemsResult.Ok -> Unit.right()
    }

    override suspend fun getCursor(
        conversationId: LocalConversationId
    ): Either<PaginationError, MailMessageCursorWrapper> = when (val result = rustPaginator.cursor(conversationId)) {
        is SearchScrollerCursorResult.Error -> result.v1.toPaginationError().left()
        is SearchScrollerCursorResult.Ok -> MailMessageCursorWrapper(result.v1).right()
    }

    override fun disconnect() {
        Timber.d("search-paginator: Disconnecting paginator with id=%s", rustPaginator.id())

        rustPaginator.watchHandle().disconnect()
        rustPaginator.terminate()
    }

    override fun filterUnread(filterUnread: Boolean) {
        Timber.w("search-paginator: Called filter unread on a search paginator, which is illegal. No-op.")
    }

    override fun showSpamAndTrash(show: Boolean) {
        Timber.d("search-paginator: Updating show spam and trash to: $show")
        val includeSwitch = if (show) IncludeSwitch.WITH_SPAM_AND_TRASH else IncludeSwitch.DEFAULT
        rustPaginator.changeInclude(includeSwitch)
    }

    override fun updateKeyword(keyword: String) {
        Timber.d("search-paginator: Updating search keyword to: $keyword")
        rustPaginator.changeKeywords(PaginatorSearchOptions(keyword))
    }

    override fun getScrollerId(): String = rustPaginator.id()

}
