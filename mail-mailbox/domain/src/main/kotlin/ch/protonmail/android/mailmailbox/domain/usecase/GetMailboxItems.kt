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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.Either
import arrow.core.getOrElse
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.mapper.ConversationMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SearchMatchPosition
import ch.protonmail.android.mailmessage.domain.model.searchMatchedTerms
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Get MailboxItems for a user, according a [PageKey].
 *
 */
class GetMailboxItems @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val messageMailboxItemMapper: MessageMailboxItemMapper,
    private val conversationMailboxItemMapper: ConversationMailboxItemMapper
) {
    suspend operator fun invoke(
        userId: UserId,
        type: MailboxItemType,
        pageKey: PageKey = PageKey.DefaultPageKey()
    ): Either<PaginationError, List<MailboxItem>> = when (type) {
        MailboxItemType.Message -> messageRepository.getMessages(userId, pageKey).map { list ->
            val matchPositions = if (pageKey is PageKey.PageKeyForSearch) {
                getSearchMatchPositions(userId, pageKey, list)
            } else {
                emptyMap()
            }
            list.map { message ->
                messageMailboxItemMapper.toMailboxItem(
                    message = message,
                    searchMatchedTerms = message.searchMatchedTerms(matchPositions[message.messageId].orEmpty())
                )
            }
        }

        MailboxItemType.Conversation -> when (pageKey) {
            is PageKey.DefaultPageKey -> conversationRepository.getConversations(userId, pageKey).map { list ->
                list.map { conversationMailboxItemMapper.toMailboxItem(it) }
            }
            is PageKey.PageKeyForSearch -> throw IllegalArgumentException("Invalid page key $pageKey for View Mode")
        }
    }

    // Highlighting is best effort.
    // A failure leaves the results without fuzzy highlights rather than failing the page.
    private suspend fun getSearchMatchPositions(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>
    ): Map<MessageId, List<SearchMatchPosition>> {
        if (messages.isEmpty()) return emptyMap()
        return messageRepository.getSearchMatchPositions(userId, pageKey, messages.map { it.messageId })
            .getOrElse {
                Timber.w("Failed to get search match positions: $it")
                emptyMap()
            }
    }
}
