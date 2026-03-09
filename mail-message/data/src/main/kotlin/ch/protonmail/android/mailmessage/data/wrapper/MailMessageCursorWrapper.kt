/*
 * Copyright (c) 2025 Proton Technologies AG
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

import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailmessage.data.mapper.toConversationCursorError
import ch.protonmail.android.mailmessage.data.mapper.toConversationId
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import uniffi.mail_uniffi.MailMessageCursor
import uniffi.mail_uniffi.MailMessageCursorFetchNextResult
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.NextMailCursorMessage

class MailMessageCursorWrapper(private val cursor: MailMessageCursor) : ConversationCursor {

    override suspend fun nextPage(): CursorResult = when (
        val result =
            cursor.peekNext()
    ) {
        is NextMailCursorMessage.None -> CursorResult.End
        is NextMailCursorMessage.Maybe -> {
            // not available sync, try the async fetch
            when (
                val asyncResult = cursor.fetchNext()
            ) {
                is MailMessageCursorFetchNextResult.Error ->
                    CursorResult.Error(asyncResult.v1.toConversationCursorError())

                is MailMessageCursorFetchNextResult.Ok -> asyncResult.v1?.toCursor() ?: CursorResult.End
            }
        }

        is NextMailCursorMessage.Some -> result.v1.toCursor()
    }

    override fun previousPage(): CursorResult = cursor.peekPrev()?.toCursor() ?: CursorResult.End

    override fun goForwards() = cursor.gotoNext()
    override fun goBackwards() = cursor.gotoPrev()

    override fun disconnect() {
        cursor.close()
    }

    private fun Message.toCursor() =
        CursorResult.Cursor(this.conversationId.toConversationId(), this.id.toMessageId().id)
}
