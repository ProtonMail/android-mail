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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.wrapper.MailMessageCursorWrapper
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.MessageScrollerStatusUpdate

interface RustMessageListQuery {
    suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<PaginationError, List<Message>>
    suspend fun terminatePaginator(userId: UserId)

    suspend fun supportsIncludeFilter(): Boolean

    suspend fun updateUnreadFilter(filterUnread: Boolean)

    suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean)

    suspend fun getCursor(
        userId: UserId,
        labelId: LabelId,
        conversationId: LocalConversationId
    ): Either<PaginationError, MailMessageCursorWrapper>?

    fun observeScrollerFetchNewStatus(): Flow<MessageScrollerStatusUpdate>

}
