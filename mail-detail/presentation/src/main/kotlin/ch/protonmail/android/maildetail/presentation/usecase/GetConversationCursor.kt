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

package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.domain.repository.SingleItemConversationCursor
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maildetail.presentation.model.ConversationPagerSource
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class GetConversationCursor @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        userId: UserId,
        conversationId: ConversationId,
        messageId: String?,
        locationViewModeIsConversation: Boolean,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?,
        pagerSource: ConversationPagerSource,
        searchQuery: String
    ): Either<ConversationCursorError, ConversationCursor> {
        val cursorId = CursorId(conversationId, messageId)

        return when (pagerSource) {
            ConversationPagerSource.Mailbox -> getMailboxCursor(
                userId = userId,
                cursorId = cursorId,
                locationViewModeIsConversation = locationViewModeIsConversation,
                labelId = labelId,
                categoryLabelId = categoryLabelId
            )

            ConversationPagerSource.SearchResults -> getSearchCursor(userId, cursorId, searchQuery).right()
            ConversationPagerSource.SingleItem -> SingleItemConversationCursor(cursorId).right()
        }
    }

    private suspend fun getMailboxCursor(
        userId: UserId,
        cursorId: CursorId,
        locationViewModeIsConversation: Boolean,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?
    ): Either<ConversationCursorError, ConversationCursor> = if (locationViewModeIsConversation) {
        conversationRepository.getConversationCursor(
            userId = userId,
            anchorItemId = cursorId,
            labelId = labelId,
            categoryLabelId = categoryLabelId
        )
    } else {
        messageRepository.getConversationCursor(
            userId = userId,
            anchorItemId = cursorId,
            labelId = labelId,
            categoryLabelId = categoryLabelId
        )
    }

    /**
     * Search results are messages, so this is a message cursor whatever the grouping.
     *
     * Without the search, only the opened result is shown. Another list would swipe to unrelated messages.
     */
    private suspend fun getSearchCursor(
        userId: UserId,
        cursorId: CursorId,
        searchQuery: String
    ): ConversationCursor = messageRepository.getSearchCursor(cursorId, userId, searchQuery).getOrElse { error ->
        Timber.d("No search results to swipe through, showing only the opened result. error=$error")
        SingleItemConversationCursor(cursorId)
    }
}
