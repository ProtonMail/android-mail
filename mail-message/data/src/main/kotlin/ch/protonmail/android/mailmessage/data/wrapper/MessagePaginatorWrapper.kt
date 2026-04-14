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
import ch.protonmail.android.mailcommon.data.mapper.LocalItemId
import ch.protonmail.android.mailpagination.domain.model.PaginationError

interface MessagePaginatorWrapper {

    suspend fun supportsIncludeFilter(): Boolean

    suspend fun nextPage(): Either<PaginationError, Unit>

    suspend fun reload(): Either<PaginationError, Unit>

    suspend fun getCursor(anchorItemId: LocalItemId): Either<PaginationError, MailMessageCursorWrapper>

    fun disconnect()

    fun filterUnread(filterUnread: Boolean)

    fun showSpamAndTrash(show: Boolean)

    fun updateKeyword(keyword: String)

    fun getScrollerId(): String
}
