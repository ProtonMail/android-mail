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

package ch.protonmail.android.mailconversation.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailconversation.data.mapper.toConversationError
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.OpenConversationOrigin
import uniffi.mail_uniffi.WatchConversationResult
import uniffi.mail_uniffi.WatchedConversation
import uniffi.mail_uniffi.watchConversation
import javax.inject.Inject

class CreateRustConversationWatcher @Inject constructor() {

    suspend operator fun invoke(
        mailbox: MailboxWrapper,
        conversationId: LocalConversationId,
        callback: LiveQueryCallback,
        origin: OpenConversationOrigin,
        showAll: Boolean
    ): Either<ConversationError, WatchedConversation> = when (
        val result = watchConversation(
            mailbox = mailbox.getRustMailbox(),
            id = conversationId,
            origin = origin,
            callback = callback,
            showAll = showAll
        )
    ) {
        is WatchConversationResult.Error -> result.v1.toConversationError().left()
        is WatchConversationResult.Ok -> {
            when (val watcher = result.v1) {
                null -> ConversationError.NullValueReturned.left()
                else -> watcher.right()
            }
        }
    }
}
