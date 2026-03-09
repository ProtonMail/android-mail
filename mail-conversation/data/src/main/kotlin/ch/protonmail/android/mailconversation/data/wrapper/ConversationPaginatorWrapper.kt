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

package ch.protonmail.android.mailconversation.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailpagination.data.mapper.toPaginationError
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import timber.log.Timber
import uniffi.mail_uniffi.ConversationScroller
import uniffi.mail_uniffi.ConversationScrollerCursorResult
import uniffi.mail_uniffi.ConversationScrollerFetchMoreResult
import uniffi.mail_uniffi.ConversationScrollerGetItemsResult
import uniffi.mail_uniffi.ConversationScrollerSupportsIncludeFilterResult
import uniffi.mail_uniffi.IncludeSwitch
import uniffi.mail_uniffi.ReadFilter

class ConversationPaginatorWrapper(private val rustPaginator: ConversationScroller) {

    suspend fun supportsIncludeFilter() = when (val result = rustPaginator.supportsIncludeFilter()) {
        is ConversationScrollerSupportsIncludeFilterResult.Error -> {
            Timber.w("conversation-paginator: failed to define supportsIncludeFilter: $result")
            false
        }

        is ConversationScrollerSupportsIncludeFilterResult.Ok -> result.v1
    }

    fun nextPage(): Either<PaginationError, Unit> = when (val result = rustPaginator.fetchMore()) {
        is ConversationScrollerFetchMoreResult.Error -> result.v1.toPaginationError().left()
        is ConversationScrollerFetchMoreResult.Ok -> Unit.right()
    }

    fun reload(): Either<PaginationError, Unit> = when (val result = rustPaginator.getItems()) {
        is ConversationScrollerGetItemsResult.Error -> result.v1.toPaginationError().left()
        is ConversationScrollerGetItemsResult.Ok -> Unit.right()
    }

    suspend fun getCursor(conversationId: LocalConversationId) =
        when (val result = rustPaginator.cursor(conversationId)) {
            is ConversationScrollerCursorResult.Error -> result.v1.toPaginationError().left()
            is ConversationScrollerCursorResult.Ok -> ConversationCursorWrapper(result.v1).right()
        }

    fun filterUnread(filterUnread: Boolean) {
        Timber.d("conversation-paginator: Changing unread filter to %s id=%s", filterUnread, rustPaginator.id())
        val filter = if (filterUnread) ReadFilter.UNREAD else ReadFilter.ALL
        rustPaginator.changeFilter(filter)
    }

    fun showSpamAndTrash(show: Boolean) {
        Timber.d("conversation-paginator: Changing show spam and trash to %s id=%s", show, rustPaginator.id())
        val includeSwitch = if (show) IncludeSwitch.WITH_SPAM_AND_TRASH else IncludeSwitch.DEFAULT
        rustPaginator.changeInclude(includeSwitch)
    }

    fun disconnect() {
        Timber.d("conversation-paginator: Disconnecting paginator with id=%s", rustPaginator.id())
        rustPaginator.watchHandle().disconnect()
        rustPaginator.terminate()
    }

    fun getScrollerId(): String = rustPaginator.id()
}
