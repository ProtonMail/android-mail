/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalRecentFoundItem
import ch.protonmail.android.mailcommon.data.mapper.LocalRecentSearchTerm
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionRecentFoundItemsResult
import uniffi.mail_uniffi.MailUserSessionRecentSearchTermsResult
import uniffi.mail_uniffi.VoidSearchRecentsResult

/**
 * The recents side of the Rust search API. The calls live on `MailUserSession`, but they are wrapped
 * here rather than in [ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper] to
 * keep the search surface with the rest of the content-search data layer.
 */
class SearchRecentsWrapper(private val userSession: MailUserSession) {

    suspend fun recentSearchTerms(prefix: String?, limit: UInt): Either<DataError, List<LocalRecentSearchTerm>> =
        when (val result = userSession.recentSearchTerms(prefix, limit)) {
            is MailUserSessionRecentSearchTermsResult.Error -> result.v1.toDataError().left()
            is MailUserSessionRecentSearchTermsResult.Ok -> result.v1.right()
        }

    suspend fun touchRecentSearchTerm(query: String): Either<DataError, Unit> =
        userSession.touchRecentSearchTerm(query).toEither()

    suspend fun dismissRecentSearchTerm(query: String): Either<DataError, Unit> =
        userSession.dismissRecentSearchTerm(query).toEither()

    suspend fun recentFoundItems(limit: UInt): Either<DataError, List<LocalRecentFoundItem>> =
        when (val result = userSession.recentFoundItems(limit)) {
            is MailUserSessionRecentFoundItemsResult.Error -> result.v1.toDataError().left()
            is MailUserSessionRecentFoundItemsResult.Ok -> result.v1.right()
        }

    suspend fun touchRecentFoundItem(messageId: LocalMessageId): Either<DataError, Unit> =
        userSession.touchRecentFoundItem(messageId).toEither()

    suspend fun dismissRecentFoundItem(messageId: LocalMessageId): Either<DataError, Unit> =
        userSession.dismissRecentFoundItem(messageId).toEither()

    suspend fun recordSearchOpen(
        query: String,
        messageId: LocalMessageId,
        conversationId: LocalConversationId
    ): Either<DataError, Unit> = userSession.recordSearchOpen(query, messageId, conversationId).toEither()

    private fun VoidSearchRecentsResult.toEither(): Either<DataError, Unit> = when (this) {
        is VoidSearchRecentsResult.Error -> v1.toDataError().left()
        VoidSearchRecentsResult.Ok -> Unit.right()
    }
}
