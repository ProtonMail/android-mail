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
import uniffi.proton_mail_uniffi.IncludeSwitch
import uniffi.proton_mail_uniffi.MessageScroller
import uniffi.proton_mail_uniffi.MessageScrollerCursorResult
import uniffi.proton_mail_uniffi.MessageScrollerFetchMoreResult
import uniffi.proton_mail_uniffi.MessageScrollerGetItemsResult
import uniffi.proton_mail_uniffi.MessageScrollerSupportsIncludeFilterResult
import uniffi.proton_mail_uniffi.ReadFilter

class MailboxMessagePaginatorWrapper(
    private val rustPaginator: MessageScroller
) : MessagePaginatorWrapper {

    override suspend fun supportsIncludeFilter() = when (val result = rustPaginator.supportsIncludeFilter()) {
        is MessageScrollerSupportsIncludeFilterResult.Error -> {
            Timber.w("message-paginator: failed to define supportsIncludeFilter: $result")
            false
        }
        is MessageScrollerSupportsIncludeFilterResult.Ok -> result.v1
    }

    override suspend fun nextPage(): Either<PaginationError, Unit> = when (val result = rustPaginator.fetchMore()) {
        is MessageScrollerFetchMoreResult.Error -> result.v1.toPaginationError().left()
        is MessageScrollerFetchMoreResult.Ok -> Unit.right()
    }

    override suspend fun reload(): Either<PaginationError, Unit> = when (val result = rustPaginator.getItems()) {
        is MessageScrollerGetItemsResult.Error -> result.v1.toPaginationError().left()
        is MessageScrollerGetItemsResult.Ok -> Unit.right()
    }

    override suspend fun getCursor(conversationId: LocalConversationId) =
        when (val result = rustPaginator.cursor(conversationId)) {
            is MessageScrollerCursorResult.Error -> result.v1.toPaginationError().left()
            is MessageScrollerCursorResult.Ok -> MailMessageCursorWrapper(result.v1).right()
        }

    override fun disconnect() {
        Timber.d("message-paginator: Disconnecting paginator with id=%s", rustPaginator.id())
        rustPaginator.watchHandle().disconnect()
        rustPaginator.terminate()
    }

    override fun filterUnread(filterUnread: Boolean) {
        Timber.d("message-paginator: Changing unread filter to %s", filterUnread)
        val filter = if (filterUnread) ReadFilter.UNREAD else ReadFilter.ALL
        rustPaginator.changeFilter(filter)
    }

    override fun showSpamAndTrash(show: Boolean) {
        Timber.d("message-paginator: Changing show spam and trash to %s", show)
        val includeSwitch = if (show) IncludeSwitch.WITH_SPAM_AND_TRASH else IncludeSwitch.DEFAULT
        rustPaginator.changeInclude(includeSwitch)
    }

    override fun updateKeyword(keyword: String) {
        Timber.w("message-paginator: Called updateKeyword on a message paginator, which is illegal. No-op.")
    }

    override fun getScrollerId(): String = rustPaginator.id()
}
